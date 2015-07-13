package io.cettia.example.di.tapestry5;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.platform.action.Action;

import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.ServiceBuilder;
import org.apache.tapestry5.ioc.ServiceResources;

public class TapestryModule {
    public static void bind(ServiceBinder binder) {
        binder.bind(Clock.class);
        binder.bind(Server.class, new ServiceBuilder<Server>() {
            @Override
            public Server buildService(ServiceResources resources) {
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
        });
    }
}
