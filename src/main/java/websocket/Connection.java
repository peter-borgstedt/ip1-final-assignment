package websocket;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import io.jsonwebtoken.Claims;
import websocket.core.WsRequestDataContext;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * A class wrapping the websocket session with some additional properties
 * retrieved during and before the handshake. These are the claims (credentials
 * and user details) and remote host, remote address and remote port.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Connection {
  private Session session;
  private Map<String, Object> properties;

  /**
   * Constructor.
   * @param session Websocket session object
   * @param context Additional data regarding the connection
   */
  public Connection (Session session, WsRequestDataContext context) {
    this.session = session;
    this.properties = new HashMap<>(context.getProperties());
  }

  /**
   * Get websocket session object.
   * @return session
   */
  public Session getSession() {
    return this.session;
  } 

  /**
   * Get websocket session id.
   * @return id
   */
  public String getId() {
    return this.session.getId();
  }

  /**
   * Get properties containing additional properties suchs as
   * claims with credentials and user details as well as remote host, address and port.
   */
  public Map<String, Object> getProperties() {
    return this.properties;
  }

  /**
   * Get server (local) host.
   * @return host
   */
  public String getServerHost() {
    return this.get("serverHost", String.class);
  }

  /**
   * Get remote (connectiono) host.
   * @return host
   */
  public String getRemoteHost() {
    return this.get("remoteHost", String.class);
  }

  /**
   * Get remote (connection) port.
   * @return port
   */
  public String getRemotePort() {
    return this.get("remotePort", String.class);
  }

  /**
   * Get the remote (connection) address.
   * @return host
   */
  public String getRemoteAddr() {
    return this.get("remoteAddr", String.class);
  }

  /**
   * Get claims retrieved from JWT.
   * @return claims
   */
  public Claims getClaims() {
    return get("claims", Claims.class);
  }

  /**
   * Get user email.
   * @return email
   */
  public String getUserEmail() {
    return String.class.cast(this.getClaims().get("email"));
  }

  /**
   * Get user id.
   * @return user id
   */
  public String getUserID() {
    return String.class.cast(this.getClaims().get("id"));
  }

  /**
   * Get if JWT has expired.
   * @return whether JWT has expired
   */
  public boolean hasExpired () {
    return getClaims().getExpiration().getTime() >= System.currentTimeMillis();
  }

  /**
   * Get casted property value.
   * @param key Parameter key
   * @param clazz Class to cast value into
   * @return casted value
   */
  public <T> T get (String key, Class<T> clazz) {
    return clazz.cast(properties.get(key));
  }
}
