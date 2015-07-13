Type:

```
mvn jetty:run
```

And in another console

```
mvn jetty:run -Djetty.port=8090
```

Then, open two [clients](http://jsbin.com/lelaba/1/watch?js,console) connecting to server at 8080 and 8090 respectively in your browser and see that chat event from server at port 8080 is propagated to server at port 8090.

**Note**


* Although this example uses Atmosphere platform, you can use JMS with any platform.
* HornetQ 2 is used as an implementation of JMS.
* You need to run HornetQ server by `mvn hornetq:start` first.