package datastructures;

import java.util.Map;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a Websocket Action request.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class WsActionRequest {
  public String type; // Type of action
  public Map<String, Object> data; // Data related to the type of action

  /** Constructor */
  public WsActionRequest() {
    // Will be populated from outside
  }
}
