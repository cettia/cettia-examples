package io.cettia.example.di.tapestry5;

import io.cettia.DefaultServer;
import io.cettia.Server;
import org.apache.tapestry5.ioc.ServiceBinder;

public class TapestryModule {
  public static void bind(ServiceBinder binder) {
    binder.bind(Clock.class);
    binder.bind(Server.class, resources -> {
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
    });
  }
}
