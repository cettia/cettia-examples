package io.cettia.example.di.cdi1;

import io.cettia.Server;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class Clock {
  // Injects the server
  @Inject
  private Server server;

  @PostConstruct
  public void init() {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(() -> tick(), 0, 3, TimeUnit.SECONDS);
  }

  public void tick() {
    server.all().send("chat", "tick: " + System.currentTimeMillis());
  }
}
