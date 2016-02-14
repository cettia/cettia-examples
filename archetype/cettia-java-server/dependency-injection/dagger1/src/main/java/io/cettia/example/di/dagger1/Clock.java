package io.cettia.example.di.dagger1;

import io.cettia.Server;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Clock {
  // Injects the server
  @Inject
  Server server;

  public void init() {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        tick();
      }
    }, 0, 3, TimeUnit.SECONDS);
  }

  public void tick() {
    server.all().send("chat", "tick: " + System.currentTimeMillis());
  }
}
