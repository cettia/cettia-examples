package io.cettia.example.platform.servlet3;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.asity.bridge.servlet3.AsityServlet;
import io.cettia.asity.http.HttpStatus;
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

    HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
    ServletContext context = event.getServletContext();
    Servlet servlet = new AsityServlet().onhttp(http -> {
      // Ignores WebSocket handshake request
      if ("websocket".equalsIgnoreCase(http.header("upgrade"))) {
        http.setStatus(HttpStatus.NOT_IMPLEMENTED).end();
      } else {
        httpTransportServer.on(http);
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
