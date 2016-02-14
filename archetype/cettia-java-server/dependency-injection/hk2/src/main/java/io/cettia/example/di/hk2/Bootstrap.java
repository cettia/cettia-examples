package io.cettia.example.di.hk2;

import io.cettia.Server;
import io.cettia.asity.bridge.atmosphere2.AsityAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import org.atmosphere.cpr.ApplicationConfig;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

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
    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
    ServiceLocatorUtilities.enableImmediateScope(locator);
    Server server = locator.getService(Server.class);

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
