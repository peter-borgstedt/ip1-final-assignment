package datastructures;

import java.util.Map;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains a Websocket Message Action, containing information
 * on what kind of message it is, the data for it and what channel
 * it is associated with.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class MessageAction {
  public String type;
  public String channelId;
  // Type is of java.util.LinkedHashMap
  public Map<String, String> data;

  /** Constructor */
  public MessageAction() {
    // Will be populated from outside
  }
}