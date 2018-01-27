package io.cettia.example.clsutering.redis2;

import io.cettia.ClusteredServer;
import io.cettia.asity.bridge.jwa1.AsityServerEndpoint;
import io.cettia.asity.bridge.servlet3.AsityServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

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
    // Receives a message
    new Thread(() -> {
      @SuppressWarnings("resource")
      Jedis jedis = new Jedis("localhost");
      jedis.subscribe(new BinaryJedisPubSub() {
        @SuppressWarnings("unchecked")
        @Override
        public void onMessage(byte[] channel, byte[] message) {
          ByteArrayInputStream bais = new ByteArrayInputStream(message);
          Map<String, Object> body = null;
          try (ObjectInputStream in = new ObjectInputStream(bais)) {
            body = (Map<String, Object>) in.readObject();
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          System.out.println("receiving a message: " + body);
          server.messageAction().on(body);
        }
      }, "cettia".getBytes());
    })
      .start();
    // Publishes a message
    server.onpublish(message -> {
      System.out.println("publishing a message: " + message);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
      try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
        out.writeObject(message);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      @SuppressWarnings("resource")
      Jedis jedis = new Jedis("localhost");
      jedis.publish("cettia".getBytes(), baos.toByteArray());
    });

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
