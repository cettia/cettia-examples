package io.cettia.example.clsutering.jms2;

import io.cettia.ClusteredServer;
import io.cettia.asity.bridge.jwa1.AsityServerEndpoint;
import io.cettia.asity.bridge.servlet3.AsityServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

@WebListener
public class Bootstrap implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent event) {
    ClusteredServer server = new ClusteredServer();
    try {
      // Set by HornetQ standalone server
      Properties props = new Properties();
      props.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      props.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
      props.put("java.naming.provider.url", "jnp://localhost:1099");
      InitialContext initialContext = new InitialContext(props);
      TopicConnectionFactory connectionFactory = (TopicConnectionFactory) initialContext.lookup
        ("ConnectionFactory");
      Topic topic = (Topic) initialContext.lookup("topic/cettia");
      TopicConnection connection = connectionFactory.createTopicConnection();
      TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();
      // Receives a message
      TopicSubscriber subscriber = session.createSubscriber(topic);
      subscriber.setMessageListener(message -> {
        try {
          System.out.println("receiving a message: " + message.getBody(Map.class));
          server.messageAction().on(message.getBody(Map.class));
        } catch (JMSException e) {
          throw new RuntimeException(e);
        }
      });
      // Publishes a message
      TopicPublisher publisher = session.createPublisher(topic);
      server.onpublish(message -> {
        System.out.println("publishing a message: " + message);
        try {
          publisher.publish(session.createObjectMessage((Serializable) message));
        } catch (JMSException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (JMSException e) {
      throw new RuntimeException(e);
    } catch (NamingException e) {
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
