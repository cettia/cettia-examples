Type:

```
mvn package vertx:runMod -Dvertx.port=8080
```

And in another console

```
mvn package vertx:runMod -Dvertx.port=8090
```

Then, open two [clients](http://jsbin.com/tafamen/1/watch?js,console) connecting to server at 8080 and 8090 respectively in your browser and see that chat event from server at port 8080 is propagated to server at port 8090.
