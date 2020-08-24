package rest.api;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import io.jsonwebtoken.Claims;
import rest.annotation.Authorization;
import rest.structures.ChannelCreateRequest;
import services.ChannelDb;
import services.DbUtils.ConstraintException;
import websocket.core.WsConfiguration;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * REST API containing resources for Channels and messages contained
 * in these channels.
 * 
 * Resources:
 * - Create channel
 * - Remove channel
 * - Subscribe on channel
 * - Unsubscribe on channel
 * - Get channel
 * - Get channels subscribed by user
 * - Get all channels
 * - Get messages of a channel (with pagination)
 *
 * References:
 * https://eclipse-ee4j.github.io/jersey.github.io/documentation/2.31/user-guide.html#jaxrs-resources
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
@Path("/channels")
public class Channels {
  private static final Logger log = LogManager.getLogger(Channels.class);

  /**
   * Creates a chat channel.
   * @param cr Request context containing the claims (credentials and user details)
   * @param body Request body containing details about the channel to be created
   * @return response with the details of the created channel
   */
  @POST
  @Authorization
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @JsonInclude(Include.NON_NULL)
  public Response createChannel(@Context ContainerRequest cr, @Valid ChannelCreateRequest body) {
    try {
      // Credentials and user details
      var claims = Claims.class.cast(cr.getProperty("claims"));
      // Use user doing the request as the creator_id
      var userId = claims.get("id", String.class);

      // Create channel in database
      var channel = ChannelDb.createChannel(body.name, body.description, userId);

      // Set the creating user as subscriber on channel
      ChannelDb.subscribeChannel(userId, channel.id);
    
      // Update the websocket service with new channel and subscription on it
      WsConfiguration.SERVER.subscribeNewChannel(userId, channel.id);

      return Response.status(200).entity(channel).build();
    } catch (ConstraintException e) {
      return Response.status(400).entity(e.getMessage()).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred").build();
    }
  }

  /**
   * Get all existing channels
   * @param cr Request context containing the claims (credentials and user details)
   * @return a list of channel records with details of channel
   */
  @GET
  @Authorization
  @Produces(MediaType.APPLICATION_JSON)
  public Response getChannels(@Context ContainerRequest cr) {
    try {
      var channels = ChannelDb.getChannels();
      return Response.status(200).entity(channels).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred").build();
    }
  }

  /**
   * Get messages with pagination.
   * @param cr Request context containing the claims (credentials and user details)
   * @param channelId The id of the channel
   * @param from Query from index
   * @param limit Amount of records to retrieve
   * @return the records and whether the query has reached the end
   */
  @GET
  @Authorization
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{channelId}")
  public Response getMessages(
    @Context ContainerRequest cr,
    @PathParam("channelId") String channelId,
    @QueryParam("from") Integer from,
    @DefaultValue("10")
    @QueryParam("limit") Integer limit
  ) {
    try {
      log.info(String.format("{ channelId: %s, from: %s, limit: %s}", channelId, from, limit));

      var messages = ChannelDb.getMessages(channelId, from, limit);
      return Response.status(200).entity(messages).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred").build();
    }
  }
}
