package websocket.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jsonwebtoken.SignatureException;
import services.TokenService;
import websocket.WsServer;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Websocket configuration.
 * 
 * Does the handshake between server and clients, validates user authenticity,
 * forwards data retrieved during the request call and forward it to the final
 * websocket connection with claims (credentials, user details) and remote host,
 * remote address and remote port etc.
 * 
 * The Websocket service is configured with shared instance for all connections
 * for an easier management of a pool of connections.
 * 
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class WsConfiguration extends ServerEndpointConfig.Configurator {
  private static final Logger log = LogManager.getLogger(WsConfiguration.class);

  // Reuse the same instance for all connections (Singleton style)
  public static final WsServer SERVER = new WsServer();

  @Override
  public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    super.modifyHandshake(config, request, response);

    var session = HttpSession.class.cast(request.getHttpSession());
    setClaims(request, session);

    try {
      // Internal server host description
      var serverHost = InetAddress.getLocalHost();
      var serverAddr = serverHost.getHostAddress();
      var serverName = serverHost.getCanonicalHostName();

      var remoteAddr = String.class.cast(session.getAttribute("remoteAddr"));

      log.info(String.format("%s <- between -> %s (%s)", remoteAddr, serverAddr, serverName));
    } catch (UnknownHostException e) {
      e.printStackTrace(); // Should not happened
    }
  }

  private void setClaims(HandshakeRequest request, HttpSession session) {
    var authorizationParameter = WsConfiguration.parseQuery(request.getRequestURI().getQuery()).get("auth");

    if (authorizationParameter != null && !authorizationParameter.isEmpty()) {
      try {
        var claims = TokenService.getInstance().parse(authorizationParameter.get(0));

        if (claims != null) {
          var properties = new HashMap<String, Object>();
          properties.put("claims", claims);
          properties.put("id", session.getId());
          properties.put("remoteHost", String.class.cast(session.getAttribute("remoteHost")));
          properties.put("remoteAddr", String.class.cast(session.getAttribute("remoteAddr")));
          properties.put("remotePort", Integer.class.cast(session.getAttribute("remotePort")));
          properties.put("serverHost", String.class.cast(session.getAttribute("serverHost")));
          WsRequestDataContext.setCurrentInstance(new WsRequestDataContext(properties));
        }
      } catch (SignatureException e) {
        e.printStackTrace();
        var remoteAddr = String.class.cast(session.getAttribute("remoteAddr"));
        log.error(String.format("Invalid JWT signature used by %s", remoteAddr));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Parses query any existing query parameters in the URL path into an easier to retrieve data structure.
   * 
   * Following example will build a hash map with key parameter1 and parameter two with value 1 and two:
   * https://www.my-site.com/path/action?parameter2=1&parameter2=2
   * 
   * @param query Query parameter
   * @return parsed query parameters
   */
  public static Map<String, List<String>> parseQuery(String query) {
    if (query == null || query.isEmpty()) {
        return Collections.emptyMap(); // No query parameters exists
    }

    // Split on query parameter delimiter "&"
    return Arrays.stream(query.split("&"))
            .map(WsConfiguration::splitQueryParameter)
            .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new,
                     Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  /**
   * Splits a string that contains a key, value and a delimiter separating them both into
   * the represented values.
   * @param str key and value with delimiter
   * @return key and value.
   */
  public static SimpleImmutableEntry<String, String> splitQueryParameter(String str) {
      final int idx = str.indexOf("="); // Delimiter
      final String key = idx > 0 ? str.substring(0, idx) : str; // Key name
      final String value = idx > 0 && str.length() > idx + 1 ? str.substring(idx + 1) : null; // Value
      return new SimpleImmutableEntry<>(key, value);
  }

  /**
   * Override the get end point instance to use a instantiated one so all
   * connections use the same instance. This makes the management of a pool of connections
   * easier.
   * https://stackoverflow.com/a/18507371
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getEndpointInstance(Class<T> endpointClass)
  throws InstantiationException {
    if (endpointClass.equals(WsServer.class)) {
      return (T)WsConfiguration.SERVER;
    }
    throw new InstantiationException();
  }
}
