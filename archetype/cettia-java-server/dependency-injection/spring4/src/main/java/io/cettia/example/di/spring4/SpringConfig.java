package io.cettia.example.di.spring4;

import io.cettia.DefaultServer;
import io.cettia.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = {"io.cettia.example.di.spring4"})
public class SpringConfig {
  // Registers the server as a component
  @Bean
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
