package io.cettia.example.di.hk2;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.asity.action.Action;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;

// Registers the server as a component
@Service
public class ServerFactory implements Factory<Server> {
  final Server server = new DefaultServer();

  @PostConstruct
  public void init() {
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
  }

  @Override
  public Server provide() {
    return server;
  }

  @Override
  public void dispose(Server instance) {
  }
}
