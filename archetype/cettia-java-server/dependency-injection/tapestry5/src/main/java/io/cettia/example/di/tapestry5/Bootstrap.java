package io.cettia.example.di.tapestry5;

import io.cettia.Server;
import io.cettia.platform.bridge.atmosphere2.CettiaAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.atmosphere.cpr.ApplicationConfig;

@WebListener
public class Bootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        RegistryBuilder builder = new RegistryBuilder();
        builder.add(TapestryModule.class);
        Registry registry = builder.build();
        registry.performRegistryStartup();        
        final Server server = registry.getService(Server.class);
        
        HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
        WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);
        
        // Installs the server on Atmosphere platform
        ServletContext context = event.getServletContext();
        Servlet servlet = new CettiaAtmosphereServlet().onhttp(httpTransportServer).onwebsocket(wsTransportServer);
        ServletRegistration.Dynamic reg = context.addServlet(CettiaAtmosphereServlet.class.getName(), servlet);
        reg.setAsyncSupported(true);
        reg.setInitParameter(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTOR, Boolean.TRUE.toString());
        reg.addMapping("/cettia");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}