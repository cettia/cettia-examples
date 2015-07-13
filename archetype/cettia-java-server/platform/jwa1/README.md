Type:

```
mvn jetty:run
```

Then, open the [client](http://jsbin.com/lelaba/1/watch?js,console) in your browser.

**Note**

* This example runs on Jetty but you can run it on other Java WebSocket API 1 implementation.
* Java WebSocket API 1 doesn't support HTTP exchange handling so that you can use only WebSocket transport with this bridge. If you want to use HTTP transports as well as WebSocket transport together, see Servlet 3 bridge.