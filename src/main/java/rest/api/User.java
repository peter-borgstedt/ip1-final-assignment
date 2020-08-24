package rest.api;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import datastructures.ChannelRecord;
import datastructures.UserInfo;
import datastructures.UserRecord;
import datastructures.UserRegistration;
import io.jsonwebtoken.Claims;
import rest.annotation.Authorization;
import rest.structures.UserRegistrationRequest;
import rest.structures.UserSigninRequest;
import services.ChannelDb;
import services.DbUtils.ConstraintException;
import services.TokenService;
import services.UserDb;
/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * REST API containing resources for Channels and messages contained
 * in these channels.
 * 
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>´
 */
@Path("/user")
public class User {
  private static final Logger log = LogManager.getLogger(User.class);

  @Context
  private UriInfo uriInfo;

  @POST
  @Path("/signin")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response login(UserSigninRequest request) {
    try {
      log.info("Invoked");

      if (request == null) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Missing body").build();
      }

      // If user does not exist the value will be null
      var user = UserDb.getUserByEmail(request.email);

      if (user == null || !request.password.equals(user.password)) {
        return Response.status(403).entity("Forbidden").build();
      }

      log.info(String.format("User logged in successfully { id: %s, forename: %s, surname: %s, email: %s }", user.id, user.forename, user.surname, user.email));

      return Response.status(200).entity(createToken(user)).build(); // Issue token
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred").build();
    }
  }

  private String createToken(UserRecord user) throws NoSuchAlgorithmException {
    var subject = user.id;
    var issuer = uriInfo.getAbsolutePath().toString(); // Service URI

    var data = new HashMap<String, Object>();
    data.put("id", user.id);
    data.put("email", user.email);

    return TokenService.getInstance().create(subject, issuer, data);
  }

  @GET
  @Authorization
  @Path("/context")
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unused")
  public Response getContext(@Context ContainerRequest cr) {
    try {
      var claims = Claims.class.cast(cr.getProperty("claims"));
      var userId = claims.get("id", String.class);

      var response = new Object() {
        public UserInfo user = new UserInfo(UserDb.getUser(userId));
        public List<ChannelRecord> channels = ChannelDb.getChannels(userId);
      };
      return Response.status(200).entity(response).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred").build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unused")
  public Response register(@Valid UserRegistrationRequest request) {
    if (request == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Missing body").build();
    }

    try {
      var response = new Object() {
        public String id = UserDb.createUser(new UserRegistration(request));
      };
      return Response.status(200).entity(response).build();
    } catch (ConstraintException e) {
      return Response.status(400).entity(e.getMessage()).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred").build();
    }
  }

  @GET
  @Authorization
  @Path("/{userId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUser(@PathParam("userId") String userId) {
    try {
      var response = new UserInfo(UserDb.getUser(userId));
      return Response.status(200).entity(response).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred").build();
    }
  }
}
