package io.cettia.example.di.cdi1;

import io.cettia.Server;
import io.cettia.asity.bridge.atmosphere2.AsityAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.atmosphere.cpr.ApplicationConfig;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

@WebListener
public class Bootstrap implements ServletContextListener {
  @Inject
  private Server server;
  // Just for eager instantiation of Clock instance
  @SuppressWarnings("unused")
  @Inject
  private Clock clock;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
    WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);

    // Installs the server on Atmosphere platform
    ServletContext context = event.getServletContext();
    Servlet servlet = new AsityAtmosphereServlet().onhttp(httpTransportServer).onwebsocket
      (wsTransportServer);
    ServletRegistration.Dynamic reg = context.addServlet(AsityAtmosphereServlet.class.getName(),
      servlet);
    reg.setAsyncSupported(true);
    reg.setInitParameter(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTOR, Boolean.TRUE.toString());
    reg.addMapping("/cettia");
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
