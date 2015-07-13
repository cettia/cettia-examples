package io.cettia.example.clsutering.jms2;

import io.cettia.ClusteredServer;
import io.cettia.ServerSocket;
import io.cettia.platform.action.Action;
import io.cettia.platform.bridge.atmosphere2.CettiaAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import org.atmosphere.cpr.ApplicationConfig;

@WebListener
public class Bootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        final ClusteredServer server = new ClusteredServer();
        try {
            // Set by HornetQ standalone server
            Properties props = new Properties();
            props.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            props.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
            props.put("java.naming.provider.url", "jnp://localhost:1099");
            InitialContext initialContext = new InitialContext(props);
            TopicConnectionFactory connectionFactory = (TopicConnectionFactory) initialContext.lookup("ConnectionFactory");
            Topic topic = (Topic) initialContext.lookup("topic/cettia");
            TopicConnection connection = connectionFactory.createTopicConnection();
            final TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
            // Receives a message
            TopicSubscriber subscriber = session.createSubscriber(topic);
            subscriber.setMessageListener(new MessageListener() {
                @SuppressWarnings("unchecked")
                @Override
                public void onMessage(Message message) {
                    try {
                        System.out.println("receiving a message: " + message.getBody(Map.class));
                        server.messageAction().on(message.getBody(Map.class));
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            // Publishes a message
            final TopicPublisher publisher = session.createPublisher(topic);
            server.onpublish(new Action<Map<String, Object>>() {
                @Override
                public void on(Map<String, Object> message) {
                    System.out.println("publishing a message: " + message);
                    try {
                        publisher.publish(session.createObjectMessage((Serializable) message));
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        
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

        HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
        WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);

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
