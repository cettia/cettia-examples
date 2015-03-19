package io.cettia.example.di.dagger1;

import io.cettia.Server;
import io.cettia.platform.bridge.atmosphere2.CettiaAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import org.atmosphere.cpr.ApplicationConfig;

import dagger.ObjectGraph;

@WebListener
public class Bootstrap implements ServletContextListener {
    @Inject
    Server server;
    @Inject
    Clock clock;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ObjectGraph objectGraph = ObjectGraph.create(new DaggerModule());
        objectGraph.inject(this);

        HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
        WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);

        // Installs the server on Atmosphere platform
        ServletContext context = event.getServletContext();
        Servlet servlet = new CettiaAtmosphereServlet().onhttp(httpTransportServer).onwebsocket(wsTransportServer);
        ServletRegistration.Dynamic reg = context.addServlet(CettiaAtmosphereServlet.class.getName(), servlet);
        reg.setAsyncSupported(true);
        reg.setInitParameter(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTOR, Boolean.TRUE.toString());
        reg.addMapping("/cettia");
        // Start scheduler
        clock.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}