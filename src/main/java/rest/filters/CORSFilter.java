package rest.filters;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * A response filter that adds CORS (Cross-Origin Resource Sharing) to the response header.
 * The CORS tells the client which origins and methods are allowed for requests and is collected
 * by the "options" request call which is done before the actually call (to verify that it is allowed).
 *
 * References:
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
 * https://stackoverflow.com/questions/28065963/how-to-handle-cors-using-jax-rs-with-jersey/28067653
 */
@Provider
public class CORSFilter implements ContainerResponseFilter {
  private static final Logger log = LogManager.getLogger(CORSFilter.class);

  @Override public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
    log.info(String.format("%s [%s]", request.getUriInfo().getPath(), request.getMethod()));
    log.info(String.format("Response entity: %s", response.getEntity()));

    response.getHeaders().add("Access-Control-Allow-Origin", "*");
    response.getHeaders().add("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization");
    response.getHeaders().add("Access-Control-Allow-Credentials", "true");
    response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
  }
}
