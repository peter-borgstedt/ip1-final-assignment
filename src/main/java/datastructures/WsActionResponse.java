package datastructures;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a response to a Websocket Action.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class WsActionResponse {
  public String type; // Type of action
  public Object data; // Data related to the type of action

  /** Constructor */
  public WsActionResponse() {
    // Will be populated from outside
  }
}
