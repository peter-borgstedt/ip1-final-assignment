package rest.filters;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rest.annotation.Authorization;
import services.TokenService;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * Authorization filter for validating the jwt on incoming request and setting a
 * new security context.
 * 
 * References:
 * https://stackoverflow.com/a/26778123
 * https://antoniogoncalves.org/2016/10/03/securing-jax-rs-endpoints-with-jwt/
 * https://www.baeldung.com/jersey-filters-interceptors
 * https://stackoverflow.com/a/40375745
 * https://stackoverflow.com/a/40375722
 * 
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
@Provider
@Authorization
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationFilter implements ContainerRequestFilter {
  private static final Logger log = LogManager.getLogger(AuthorizationFilter.class);

  /**
   * Filter out any incoming requests that does not fulfills the jwt validation by
   * it either being missing in the header or being invalid.
   */
  @Override
  public void filter(final ContainerRequestContext ctx) throws IOException {
    var claimsJws = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);

    try {
      var claims = TokenService.getInstance().parse(claimsJws); // Parse and retrieve the claims from the JWT

      if (claims == null) {
        ctx.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Authorization header not found").build());
        return;
      }
      ctx.setProperty("claims", claims);

      ctx.setSecurityContext(new SecurityContext() {
        @Override
        public Principal getUserPrincipal() {
          return () -> {
            var username = claims.get("username", String.class);
            log.info(String.format("{ username: %s }", username));
            return username;
          };
        }

        @Override
        public boolean isUserInRole(final String role) {
          return true;
        }

        @Override
        public boolean isSecure() {
          return false; // No SSL here, but is set outside
        }

        @Override
        public String getAuthenticationScheme() {
          return TokenService.AUTHENTICATION_SCHEME;
        }
      });
    } catch (Exception e) {
      // Expired, malformed or invalid in other forms
      ctx.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Access forbidden").build());
    }
  }
}
