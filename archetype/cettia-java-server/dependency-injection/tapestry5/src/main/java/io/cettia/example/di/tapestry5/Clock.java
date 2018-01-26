package io.cettia.example.di.tapestry5;

import io.cettia.Server;
import org.apache.tapestry5.ioc.annotations.EagerLoad;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@EagerLoad
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
