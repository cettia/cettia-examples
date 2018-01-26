package io.cettia.example.clsutering.redis2;

import io.cettia.ClusteredServer;
import io.cettia.ServerSocket;
import io.cettia.asity.action.Action;
import io.cettia.asity.bridge.atmosphere2.AsityAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.atmosphere.cpr.ApplicationConfig;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
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
    final ClusteredServer server = new ClusteredServer();
    // Receives a message
    new Thread(() -> {
      @SuppressWarnings("resource")
      Jedis jedis = new Jedis("localhost");
      jedis.subscribe(new BinaryJedisPubSub() {
        @Override
        public void onUnsubscribe(byte[] channel, int subscribedChannels) {
        }

        @Override
        public void onSubscribe(byte[] channel, int subscribedChannels) {
        }

        @Override
        public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
        }

        @Override
        public void onPSubscribe(byte[] pattern, int subscribedChannels) {
        }

        @Override
        public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
        }

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
        System.out.println("on echo event: " + data);
        socket.send("echo", data);
      });
      socket.on("chat", data -> {
        System.out.println("on chat event: " + data);
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
