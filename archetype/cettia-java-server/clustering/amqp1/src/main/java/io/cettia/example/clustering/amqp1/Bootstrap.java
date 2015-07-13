package io.cettia.example.clustering.amqp1;

import io.cettia.ClusteredServer;
import io.cettia.ServerSocket;
import io.cettia.platform.action.Action;
import io.cettia.platform.bridge.atmosphere2.CettiaAtmosphereServlet;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import org.atmosphere.cpr.ApplicationConfig;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

@WebListener
public class Bootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        final ClusteredServer server = new ClusteredServer();
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            Connection connection = connectionFactory.newConnection();
            final Channel channel = connection.createChannel();
            channel.exchangeDeclare("cettia", "topic");
            String queue = channel.queueDeclare().getQueue();
            channel.queueBind(queue, "cettia", "server");
            // Receives a message
            channel.basicConsume(queue, new DefaultConsumer(channel) {
                @SuppressWarnings("unchecked")
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties props, byte[] body) throws IOException {
                    if (envelope.getRoutingKey().equals("server")) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(body);
                        Map<String, Object> message = null;
                        try (ObjectInputStream in = new ObjectInputStream(bais)) {
                            message = (Map<String, Object>) in.readObject();
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("receiving a message: " + message);
                        server.messageAction().on(message);
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    }
                }
            });
            // Publishes a message
            server.onpublish(new Action<Map<String, Object>>() {
                @Override
                public void on(Map<String, Object> message) {
                    System.out.println("publishing a message: " + message);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
                    try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
                        out.writeObject(message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        channel.basicPublish("cettia", "server", MessageProperties.BASIC, baos.toByteArray());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
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
