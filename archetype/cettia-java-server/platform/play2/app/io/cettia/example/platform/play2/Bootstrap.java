package io.cettia.example.platform.play2;

import io.cettia.DefaultServer;
import io.cettia.Server;
import io.cettia.ServerSocket;
import io.cettia.platform.action.Action;
import io.cettia.platform.bridge.play2.PlayServerHttpExchange;
import io.cettia.platform.bridge.play2.PlayServerWebSocket;
import io.cettia.transport.http.HttpTransportServer;
import io.cettia.transport.websocket.WebSocketTransportServer;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.WebSocket;

public class Bootstrap extends Controller {
    static Server server = new DefaultServer();
    static HttpTransportServer httpTransportServer = new HttpTransportServer().ontransport(server);
    static WebSocketTransportServer wsTransportServer = new WebSocketTransportServer().ontransport(server);
    static {
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
    }

    @BodyParser.Of(BodyParser.Raw.class)
    public static Promise<Result> http() {
        PlayServerHttpExchange http = new PlayServerHttpExchange(request(), response());
        httpTransportServer.on(http);
        return http.result();
    }
    
    public static WebSocket<String> websocket() {
        final Request request = request();
        return new WebSocket<String>() {
            @Override
            public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
                wsTransportServer.on(new PlayServerWebSocket(request, in, out));
            }
        };
    }
}
