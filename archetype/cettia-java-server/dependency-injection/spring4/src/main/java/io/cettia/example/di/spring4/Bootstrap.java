package io.cettia.example.di.spring4;

import io.cettia.Server;
import io.cettia.asity.bridge.jwa1.AsityServerEndpoint;
import io.cettia.asity.bridge.servlet3.AsityServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

@WebListener
public class Bootstrap implements ServletContextListener {
  @Override
  @SuppressWarnings({"resource"})
  public void contextInitialized(ServletContextEvent event) {
    AnnotationConfigApplicationContext applicationContext = new
      AnnotationConfigApplicationContext(SpringConfig.class);
    Server server = applicationContext.getBean(Server.class);

    HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
    // Servlet
    ServletContext context = event.getServletContext();
    Servlet servlet = new AsityServlet().onhttp(httpTransportServer);
    ServletRegistration.Dynamic reg = context.addServlet(AsityServlet.class.getName(), servlet);
    reg.setAsyncSupported(true);
    reg.addMapping("/cettia");

    WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport
      (server);
    // Java WebSocket API
    ServerContainer container = (ServerContainer) context.getAttribute(ServerContainer.class
      .getName());
    ServerEndpointConfig config = ServerEndpointConfig.Builder.create(AsityServerEndpoint.class,
      "/cettia")
      .configurator(new ServerEndpointConfig.Configurator() {
        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
          return endpointClass.cast(new AsityServerEndpoint().onwebsocket(wsTransportServer));
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
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
