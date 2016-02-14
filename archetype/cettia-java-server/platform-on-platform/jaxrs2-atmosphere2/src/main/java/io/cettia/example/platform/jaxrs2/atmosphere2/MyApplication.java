package io.cettia.example.platform.jaxrs2.atmosphere2;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("jaxrs")
public class MyApplication extends ResourceConfig {
  public MyApplication() {
    packages("io.cettia.example.platform.jaxrs2.atmosphere2");
  }
}
