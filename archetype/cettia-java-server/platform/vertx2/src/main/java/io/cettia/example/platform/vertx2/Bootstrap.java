package io.cettia.example.platform.vertx2;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.asity.bridge.vertx2.AsityRequestHandler;
import io.cettia.asity.bridge.vertx2.AsityWebSocketHandler;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public class Bootstrap extends Verticle {
  @Override
  public void start() {
    Server server = new DefaultServer();
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
    httpServer.listen(8080);
  }
}
