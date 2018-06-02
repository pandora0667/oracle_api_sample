package io.wisoft.oracle.rest.hello;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/hello")
public class HelloWorldResource {
  @GET
  @Produces("text/plain")
  public String getClientMessage() {
    return "Hello World";
  }
}
