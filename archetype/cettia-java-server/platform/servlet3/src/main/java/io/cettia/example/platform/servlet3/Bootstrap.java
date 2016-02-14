package io.cettia.example.platform.servlet3;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.asity.action.Action;
import io.cettia.asity.bridge.servlet3.AsityServlet;
import io.cettia.asity.http.HttpStatus;
import io.cettia.asity.http.ServerHttpExchange;
import io.cettia.transport.http.HttpTransportServer;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

@WebListener
public class Bootstrap implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent event) {
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

    final HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
    ServletContext context = event.getServletContext();
    Servlet servlet = new AsityServlet().onhttp(new Action<ServerHttpExchange>() {
      @Override
      public void on(ServerHttpExchange http) {
        // Ignores WebSocket handshake request
        if ("websocket".equalsIgnoreCase(http.header("upgrade"))) {
          http.setStatus(HttpStatus.NOT_IMPLEMENTED).end();
        } else {
          httpTransportServer.on(http);
        }
      }
    });
    ServletRegistration.Dynamic reg = context.addServlet(AsityServlet.class.getName(), servlet);
    reg.setAsyncSupported(true);
    reg.addMapping("/cettia");
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
