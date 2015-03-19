package io.cettia.example.platform.servlet3_jwa1;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.platform.action.Action;
import io.cettia.platform.bridge.jwa1.CettiaServerEndpoint;
import io.cettia.platform.bridge.servlet3.CettiaServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

@WebListener
public class Bootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
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
        
        HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
        // Servlet
        ServletContext context = event.getServletContext();
        Servlet servlet = new CettiaServlet().onhttp(httpTransportServer);
        ServletRegistration.Dynamic reg = context.addServlet(CettiaServlet.class.getName(), servlet);
        reg.setAsyncSupported(true);
        reg.addMapping("/cettia");
        
        final WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);
        // Java WebSocket API
        ServerContainer container = (ServerContainer) context.getAttribute(ServerContainer.class.getName());
        ServerEndpointConfig config = ServerEndpointConfig.Builder.create(CettiaServerEndpoint.class, "/cettia")
        .configurator(new Configurator() {
            @Override
            public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                return endpointClass.cast(new CettiaServerEndpoint().onwebsocket(wsTransportServer));
            }
        })
        .build();
        try {
            container.addEndpoint(config);
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}
