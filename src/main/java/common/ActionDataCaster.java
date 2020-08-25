package common;

import java.util.Map;

import datastructures.MessageAction;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * A class with some static methods for extracting data from
 * a special formed message representing an Action which contains
 * some data representing that action.
 * These action messages are received from the client and sent
 * to the client.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class ActionDataCaster {
  /** Private constructor */
  private ActionDataCaster() {
    throw new InstantiationError("Forbidden instantiation");
  }

  public static MessageAction getMessage(Map<String, Object> data) {
    // Refactor into a parsing class?
    var message = new MessageAction();
    message.channelId = String.class.cast(data.get("channelId"));
    message.type = String.class.cast(data.get("type"));
    message.data = ActionDataCaster.uncheckedCast(data.get("content"));
    return message;
  }

  @SuppressWarnings("unchecked")
  private static <T> T uncheckedCast(Object object) {
    return (T)object;
  }
}