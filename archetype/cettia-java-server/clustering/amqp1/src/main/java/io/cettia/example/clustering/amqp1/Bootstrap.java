package io.cettia.example.clustering.amqp1;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import io.cettia.ClusteredServer;
import io.cettia.asity.bridge.jwa1.AsityServerEndpoint;
import io.cettia.asity.bridge.servlet3.AsityServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

@WebListener
public class Bootstrap implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent event) {
    ClusteredServer server = new ClusteredServer();
    try {
      ConnectionFactory connectionFactory = new ConnectionFactory();
      Connection connection = connectionFactory.newConnection();
      Channel channel = connection.createChannel();
      channel.exchangeDeclare("cettia", "topic");
      String queue = channel.queueDeclare().getQueue();
      channel.queueBind(queue, "cettia", "server");
      // Receives a message
      channel.basicConsume(queue, new DefaultConsumer(channel) {
        @SuppressWarnings("unchecked")
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties
          props, byte[] body) throws IOException {
          if (envelope.getRoutingKey().equals("server")) {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            Map<String, Object> message = null;
            try (ObjectInputStream in = new ObjectInputStream(bais)) {
              message = (Map<String, Object>) in.readObject();
            } catch (ClassNotFoundException e) {
              throw new RuntimeException(e);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            System.out.println("receiving a message: " + message);
            server.messageAction().on(message);
            channel.basicAck(envelope.getDeliveryTag(), false);
          }
        }
      });
      // Publishes a message
      server.onpublish(message -> {
        System.out.println("publishing a message: " + message);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
          out.writeObject(message);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        try {
          channel.basicPublish("cettia", "server", MessageProperties.BASIC, baos.toByteArray());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (IOException e) {
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
