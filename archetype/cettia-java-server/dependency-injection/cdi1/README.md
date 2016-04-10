Type:

```
mvn jetty:run
```

Then, open the [client](http://jsbin.com/nirocovofe/1/edit?js,console) in your browser.

**Note**

* Though this example uses Atmosphere platform, you can use CDI with any platform.
* [Weld 2](http://weld.cdi-spec.org/) is used as an implementation of [CDI 1](cdi-spec.org).
* Maven 3.2.2+ is required because of [LinkageError issue](https://jira.codehaus.org/browse/MNG-5620).
