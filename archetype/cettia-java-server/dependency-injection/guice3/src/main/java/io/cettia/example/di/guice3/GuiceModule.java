package io.cettia.example.di.guice3;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.cettia.DefaultServer;
import io.cettia.Server;

import javax.inject.Singleton;

public class GuiceModule extends AbstractModule {
  @Override
  protected void configure() {
  }

  // Registers the server as a component
  @Provides
  @Singleton
  Server server() {
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
