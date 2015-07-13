package io.cettia.example.di.hk2;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.platform.action.Action;

import javax.annotation.PostConstruct;

import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

// Registers the server as a component
@Service
public class ServerFactory implements Factory<Server> {
    final Server server = new DefaultServer();

    @PostConstruct
    public void init() {
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
    }

    @Override
    public Server provide() {
        return server;
    }

    @Override
    public void dispose(Server instance) {}
}
