package io.wisoft.oracle.rest.containter;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.samples.storageservice.Container;
import com.sun.jersey.samples.storageservice.Item;
import com.sun.jersey.samples.storageservice.MemoryStore;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.*;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Date;
import java.util.GregorianCalendar;

public class ItemResource {
  private UriInfo uriInfo;
  private Request request;
  private String container;
  private String item;

  public ItemResource(final UriInfo uriInfo, final Request request, final String container, final String item) {
    this.uriInfo = uriInfo;
    this.request = request;
    this.container = container;
    this.item = item;
  }

  @GET
  public Response getItem() {
    System.out.println("GET ITEM + " + container + " " + item);

    Item itemResource = MemoryStore.MS.getItem(container, item);
    if (itemResource == null) {
      throw new NotFoundException("Item Not Found");
    }

    Date lastModified = itemResource.getLastModified().getTime();
    EntityTag entityTag = new EntityTag(itemResource.getDigest());
    Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModified, entityTag);
    if (responseBuilder != null) {
      return responseBuilder.build();
    }

    byte[] bytes = MemoryStore.MS.getItemData(container, item);
    return Response.ok(bytes, itemResource.getMimeType()).lastModified(lastModified).tag(entityTag).build();
  }

  @PUT
  public Response putItem(final @Context HttpHeaders headers, final byte[] data) {
    System.out.println("PUT ITEM " + container + " " + item);

    URI uri = uriInfo.getAbsolutePath();
    MediaType mimeType = headers.getMediaType();
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.set(GregorianCalendar.MILLISECOND, 0);
    Item itemResource = new Item(item, uri.toString(), mimeType.toString(), gregorianCalendar);
    String digest = computeDigest(data);
    itemResource.setDigest(digest);

    Response response;
    if (!MemoryStore.MS.hasItem(container, item)) {
      response = Response.created(uri).build();
    } else {
      response = Response.noContent().build();
    }

    Item itemResource2 = MemoryStore.MS.createOrUpdateItem(container, itemResource, data);
    if (itemResource2 == null) {
      URI containerUri = uriInfo.getAbsolutePathBuilder().path("..").build().normalize();
      Container containerResource = new Container(container, containerUri.toString());

      MemoryStore.MS.createContainer(containerResource);
      itemResource = MemoryStore.MS.createOrUpdateItem(container, itemResource, data);
      if (itemResource == null) {
        throw new NotFoundException("Container not found");
      }
    }

    return response;
  }

  @DELETE
  public void deleteItem() {
    System.out.println("DELETE ITEM " + container + " " + item);

    Item itemResource = MemoryStore.MS.deleteItem(container, item);
    if (itemResource == null) {
      throw new NotFoundException("Item Not Found");
    }
  }

  private String computeDigest(final byte[] content) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA");
      byte[] digest = md.digest(content);
      BigInteger bi = new BigInteger(digest);
      return bi.toString(16);
    } catch (Exception e) {
      return "";
    }
  }
}
