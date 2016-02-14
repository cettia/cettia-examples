package io.cettia.example.clsutering.hazelcast3;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.instance.HazelcastInstanceFactory;
import io.cettia.ClusteredServer;
import io.cettia.ServerSocket;
import io.cettia.asity.action.Action;
import io.cettia.asity.bridge.atmosphere2.AsityAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.atmosphere.cpr.ApplicationConfig;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import java.util.Map;

@WebListener
public class Bootstrap implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent event) {
    final ClusteredServer server = new ClusteredServer();
    HazelcastInstance hazelcast = HazelcastInstanceFactory.newHazelcastInstance(new Config());
    final ITopic<Map<String, Object>> topic = hazelcast.getTopic("cettia");
    // Receives a message
    topic.addMessageListener(new MessageListener<Map<String, Object>>() {
      @Override
      public void onMessage(Message<Map<String, Object>> message) {
        System.out.println("receiving a message: " + message.getMessageObject());
        server.messageAction().on(message.getMessageObject());
      }
    });
    // Publishes a message
    server.onpublish(new Action<Map<String, Object>>() {
      @Override
      public void on(Map<String, Object> message) {
        System.out.println("publishing a message: " + message);
        topic.publish(message);
      }
    });

    server.onsocket(new Action<ServerSocket>() {
      @Override
      public void on(final ServerSocket socket) {
        socket.on("echo", new Action<Object>() {
          @Override
          public void on(Object data) {
            System.out.println("on echo event: " + data);
            socket.send("echo", data);
          }
        });
        socket.on("chat", new Action<Object>() {
          @Override
          public void on(Object data) {
            System.out.println("on chat event: " + data);
            server.all().send("chat", data);
          }
        });
      }
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
