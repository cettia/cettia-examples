package io.cettia.example.clsutering.jgroups3;

import io.cettia.ClusteredServer;
import io.cettia.asity.bridge.jwa1.AsityServerEndpoint;
import io.cettia.asity.bridge.servlet3.AsityServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
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
    // Servlet
    ServletContext context = event.getServletContext();
    Servlet servlet = new AsityServlet().onhttp(httpTransportServer);
    ServletRegistration.Dynamic reg = context.addServlet(AsityServlet.class.getName(), servlet);
    reg.setAsyncSupported(true);
    reg.addMapping("/cettia");

    WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport
      (server);
    // Java WebSocket API
    ServerContainer container = (ServerContainer) context.getAttribute(ServerContainer.class
      .getName());
    ServerEndpointConfig config = ServerEndpointConfig.Builder.create(AsityServerEndpoint.class,
      "/cettia")
      .configurator(new ServerEndpointConfig.Configurator() {
        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
          return endpointClass.cast(new AsityServerEndpoint().onwebsocket(wsTransportServer));
        }
      })
      .build();
    try {
      container.addEndpoint(config);
    } catch (DeploymentException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
