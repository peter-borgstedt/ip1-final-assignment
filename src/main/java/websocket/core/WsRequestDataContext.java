package websocket.core;

import java.util.Map;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * A class storing context temporarily between from handshake to the
 * actually opening websocket connection.
 *
 * Referencess:
 * https://github.com/eclipse-ee4j/websocket-api/issues/235
 * https://stackoverflow.com/a/17994303
 * https://github.com/javaee-samples/javaee7-samples/blob/master/websocket/endpoint-singletonhttps://github.com/javaee-samples/javaee7-samples/blob/master/websocket/endpoint-singleton
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class WsRequestDataContext implements AutoCloseable {
  private static final ThreadLocal<WsRequestDataContext> INSTANCE = new ThreadLocal<>() {
    @Override
    protected WsRequestDataContext initialValue() {
      return null;
    }
  };

  private final Map<String, Object> properties;

  public WsRequestDataContext(Map<String, Object> properties) {
    this.properties = properties;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }
 
  public static WsRequestDataContext getCurrentInstance() {
    var instance = INSTANCE.get();
    if (instance == null) {
      throw new NullPointerException();
    }
    return instance;
  }

  public static void setCurrentInstance(WsRequestDataContext context) {
    if (context == null) {
      INSTANCE.remove();
    } else {
      INSTANCE.set(context);
    }
  }

  @Override
  public void close() {
    INSTANCE.remove();
  }
}
