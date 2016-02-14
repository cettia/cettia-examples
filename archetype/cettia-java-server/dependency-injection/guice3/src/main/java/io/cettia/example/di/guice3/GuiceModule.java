package io.cettia.example.di.guice3;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.asity.action.Action;

import javax.inject.Singleton;

public class GuiceModule extends AbstractModule {
  @Override
  protected void configure() {
  }

  // Registers the server as a component
  @Provides
  @Singleton
  Server server() {
    final Server server = new DefaultServer();
    server.onsocket(new Action<ServerSocket>() {
      @Override
      public void on(final ServerSocket socket) {
        socket.on("echo", new Action<Object>() {
          @Override
          public void on(Object data) {
            System.out.println("on echo event: " + data);
            socket.send("echo", data);
          }
        });
        socket.on("chat", new Action<Object>() {
          @Override
          public void on(Object data) {
            System.out.println("on chat event: " + data);
            server.all().send("chat", data);
          }
        });
      }
    });
    return server;
  }
}
