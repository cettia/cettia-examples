Type:

```
mvn jetty:run
```

And in another console

```
mvn jetty:run -Djetty.port=8090
```

Then, open two [clients](http://jsbin.com/nirocovofe/1/edit?js,console) connecting to server at 8080 and 8090 respectively in your browser and see that chat event from server at port 8080 is propagated to server at port 8090.

**Note**

* Although this example uses Atmosphere platform, you can use Redis with any platform.
* You need to run Redis instance running on `localhost` at port `6379` first.
