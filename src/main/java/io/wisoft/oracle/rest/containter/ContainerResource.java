package io.wisoft.oracle.rest.containter;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.samples.storageservice.Container;
import com.sun.jersey.samples.storageservice.Item;
import com.sun.jersey.samples.storageservice.MemoryStore;
import com.sun.jersey.samples.storageservice.resources.ItemResource;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Iterator;


@Produces("application/xml")
public class ContainerResource {
  private @Context UriInfo uriInfo;
  private @Context Request request;
  private String container;

  ContainerResource(final UriInfo uriInfo, final Request request, final String container) {
    this.uriInfo = uriInfo;
    this.request = request;
    this.container = container;
  }

  @GET
  public Container getContainer(final @QueryParam("search") String search) {
    System.out.println("GET CONTAINER " + container + "search = " + search);

    Container containerResource = MemoryStore.MS.getContainer(container);

    if (containerResource == null) {
      throw new NotFoundException("Container Not Found");
    }

    if (search != null) {
      containerResource = containerResource.clone();
      Iterator<Item> i = containerResource.getItem().iterator();
      byte[] searchBytes = search.getBytes();

      while (i.hasNext()) {
        if (!match(searchBytes, container, i.next().getName())) {
          i.remove();
        }
      }
    }
    return containerResource;
  }

  @PUT
  public Response putContainer() {
    System.out.println("PUT CONTAINER " + container);

    URI uri = uriInfo.getAbsolutePath();
    Container containerResource = new Container(container, uri.toString());

    Response response;
    if (!MemoryStore.MS.hasContainer(containerResource)) {
      response = Response.created(uri).build();
    } else {
      response = Response.noContent().build();
    }

    MemoryStore.MS.createContainer(containerResource);
    return response;
  }

  @DELETE
  public void deleteContainer() {
    System.out.println("DELETE CONTAINER " + container);

    Container containerResource = MemoryStore.MS.deleteContainer(container);
    if (containerResource == null) {
      throw new NotFoundException("Container Not Found");
    }
  }

  @Path("{item: .+}")
  public ItemResource getItemResource(final @PathParam("item") String item) {
    return new ItemResource(uriInfo, request, container, item);
  }

  private boolean match(final byte[] search, final String container, final String item) {
    byte[] bytes = MemoryStore.MS.getItemData(container, item);

    OUTER:
    for (int i = 0; i < bytes.length - search.length; i++) {
      for (int j = 0; j < search.length; j++) {
        if (bytes[i + j] != search[j])
          continue OUTER;
      }
      return  true;
    }
    return false;
  }
}
