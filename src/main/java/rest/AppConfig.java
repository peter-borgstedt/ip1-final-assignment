package rest;

import javax.ws.rs.ApplicationPath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;

import rest.filters.CORSFilter;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 * 
 * Holds the configuration and initiation of the REST service.
 * Using servlet 3.x containers which allow the use of annotations instead of needing
 * to resources define resources in: "WEB-INF/web.xml".
 *
 * References:
 * https://javadoc.io/doc/org.glassfish.jersey.core/jersey-server/2.3.1/org/glassfish/jersey/server/ResourceConfig.html
 * https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/deployment.html#deployment.servlet.3
 */
@ApplicationPath("rest")
public class AppConfig extends ResourceConfig {
  private static final Logger log = LogManager.getLogger(AppConfig.class);

  public AppConfig() {
    packages(true, "rest");
    register(new CORSFilter());
    log.debug("Application Servlet Started");
  }
}
