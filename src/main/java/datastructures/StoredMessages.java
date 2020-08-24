package datastructures;

import java.util.List;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * A collection of stored message of a channel that has been
 * retrieved from the database table Channel.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class StoredMessages {
  public boolean hasMore; // If there are more messages to be loaded (pagination)
  public List<MessageRecord> records;

  /** Constructor */
  public StoredMessages() {
    // Will be populated from outside
  }
}
