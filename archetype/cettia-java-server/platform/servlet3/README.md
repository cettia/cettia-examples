Type:

```
mvn jetty:run
```

Then, open the [client](http://jsbin.com/mupohupufi/1/edit?js,console) in your browser.

**Note**

* This example runs on Jetty but you can run it on other Servlet 3 implementation.
* Servlet 3 doesn't support WebSocket so that you can use only HTTP transports with this bridge and should reject WebSocket handshake request. If you want to use WebSocket transport as well as HTTP transports together, see Java WebSocket API 1 bridge.
