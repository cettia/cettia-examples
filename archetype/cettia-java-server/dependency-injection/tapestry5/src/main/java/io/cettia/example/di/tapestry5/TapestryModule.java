package io.cettia.example.di.tapestry5;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.asity.action.Action;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.ServiceBuilder;
import org.apache.tapestry5.ioc.ServiceResources;

public class TapestryModule {
  public static void bind(ServiceBinder binder) {
    binder.bind(Clock.class);
    binder.bind(Server.class, resources -> {
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
      return server;
    });
  }
}
