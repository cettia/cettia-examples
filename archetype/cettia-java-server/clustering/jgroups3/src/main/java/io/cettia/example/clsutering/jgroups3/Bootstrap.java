package io.cettia.example.clsutering.jgroups3;

import io.cettia.ClusteredServer;
import io.cettia.asity.bridge.atmosphere2.AsityAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.atmosphere.cpr.ApplicationConfig;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import java.util.Map;

@WebListener
public class Bootstrap implements ServletContextListener {
  @SuppressWarnings({"resource"})
  @Override
  public void contextInitialized(ServletContextEvent event) {
    ClusteredServer server = new ClusteredServer();
    JChannel channel;
    try {
      channel = new JChannel();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Receives a message
    channel.setReceiver(new ReceiverAdapter() {
      @SuppressWarnings("unchecked")
      @Override
      public void receive(Message message) {
        System.out.println("receiving a message: " + message.getObject());
        server.messageAction().on((Map<String, Object>) message.getObject());
      }
    });
    // Publishes a message
    server.onpublish(message -> {
      System.out.println("publishing a message: " + message);
      try {
        channel.send(new Message(null, message));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    try {
      channel.connect("cettia");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    server.onsocket(socket -> {
      socket.on("echo", data -> {
        System.out.println("on echo " + data);
        socket.send("echo", data);
      });
      socket.on("chat", data -> {
        System.out.println("on chat " + data);
        server.all().send("chat", data);
      });
    });

    HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
    WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);

    ServletContext context = event.getServletContext();
    Servlet servlet = new AsityAtmosphereServlet().onhttp(httpTransportServer).onwebsocket
      (wsTransportServer);
    ServletRegistration.Dynamic reg = context.addServlet(AsityAtmosphereServlet.class.getName(),
      servlet);
    reg.setAsyncSupported(true);
    reg.setInitParameter(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTOR, Boolean.TRUE.toString());
    reg.addMapping("/cettia");
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
