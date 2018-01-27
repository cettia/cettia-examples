package io.cettia.example.clsutering.vertx2;

import io.cettia.ClusteredServer;
import io.cettia.asity.bridge.vertx2.AsityRequestHandler;
import io.cettia.asity.bridge.vertx2.AsityWebSocketHandler;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public class Bootstrap extends Verticle {
  @Override
  public void start() {
    final ClusteredServer server = new ClusteredServer();
    // You need to set cluster configuration from vertx-maven-plugin to true to enable
    // distributed event bus
    final EventBus eventBus = vertx.eventBus();
    // Receives a message
    eventBus.registerHandler("cettia", (Handler<Message<byte[]>>) message -> {
      // Message's body's type bytes generated from Map object
      // retrieve original object
      ByteArrayInputStream bais = new ByteArrayInputStream(message.body());
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
    });
    // Publishes a message
    server.onpublish(message -> {
      System.out.println("publishing a message: " + message);
      // EventBus doesn't allow to publish object though it is Serializable
      // so convert it to byte array and publish it instead
      ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
      try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
        out.writeObject(message);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      eventBus.publish("cettia", baos.toByteArray());
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
    WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);

    HttpServer httpServer = vertx.createHttpServer();
    RouteMatcher httpMatcher = new RouteMatcher();
    httpMatcher.all("/cettia", new AsityRequestHandler().onhttp(httpTransportServer));
    httpServer.requestHandler(httpMatcher);
    final AsityWebSocketHandler websocketHandler = new AsityWebSocketHandler().onwebsocket
      (wsTransportServer);
    httpServer.websocketHandler(socket -> {
      if (socket.path().equals("/cettia")) {
        websocketHandler.handle(socket);
      }
    });
    httpServer.listen(Integer.parseInt(System.getProperty("vertx.port")));
  }
}
