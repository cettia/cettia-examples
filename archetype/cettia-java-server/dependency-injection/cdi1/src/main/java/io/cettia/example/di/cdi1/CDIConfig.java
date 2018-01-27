package io.cettia.example.di.cdi1;

import io.cettia.DefaultServer;
import io.cettia.Server;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class CDIConfig {
  // Registers the server as a component
  @Produces
  @Singleton
  public Server server() {
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
    return server;
  }
}
