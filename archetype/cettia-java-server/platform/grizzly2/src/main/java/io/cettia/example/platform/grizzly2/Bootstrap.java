package io.cettia.example.platform.grizzly2;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.asity.action.Action;
import io.cettia.asity.bridge.grizzly2.AsityHttpHandler;
import io.cettia.asity.bridge.grizzly2.AsityWebSocketApplication;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;

public class Bootstrap {
  public static void main(String[] args) throws Exception {
    final Server server = new DefaultServer();
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

    HttpServer httpServer = HttpServer.createSimpleServer();
    ServerConfiguration config = httpServer.getServerConfiguration();
    config.addHttpHandler(new AsityHttpHandler().onhttp(httpTransportServer), "/cettia");
    NetworkListener listener = httpServer.getListener("grizzly");
    listener.registerAddOn(new WebSocketAddOn());
    WebSocketEngine.getEngine().register("", "/cettia", new AsityWebSocketApplication()
      .onwebsocket(wsTransportServer));
    httpServer.start();
    System.in.read();
  }
}
