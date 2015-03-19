package io.cettia.example.di.spring4;

import io.cettia.Server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Clock {
    // Injects the server
    @Autowired
    private Server server;

    @Scheduled(fixedRate = 3000)
    public void tick() {
        server.all().send("chat", "tick: " + System.currentTimeMillis());
    }
}