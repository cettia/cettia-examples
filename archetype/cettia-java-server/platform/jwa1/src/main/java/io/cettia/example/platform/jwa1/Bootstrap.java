package io.cettia.example.platform.jwa1;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.platform.action.Action;
import io.cettia.platform.bridge.jwa1.CettiaServerEndpoint;
import io.cettia.transport.websocket.WebSocketTransportServer;

import java.util.Collections;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

public class Bootstrap implements ServerApplicationConfig {
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> _) {
        final Server server = new DefaultServer();
        server.onsocket(new Action<ServerSocket>() {
            @Override
            public void on(final ServerSocket socket) {
                System.out.println("on socket: " + socket.uri());
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
        
        final WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);
        ServerEndpointConfig config = ServerEndpointConfig.Builder.create(CettiaServerEndpoint.class, "/cettia")
        .configurator(new Configurator() {
            @Override
            public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                return endpointClass.cast(new CettiaServerEndpoint().onwebsocket(wsTransportServer));
            }
        })
        .build();
        return Collections.singleton(config);
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return null;
    }
}
