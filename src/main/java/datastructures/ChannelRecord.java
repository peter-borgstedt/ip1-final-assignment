package datastructures;

import java.math.BigDecimal;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a record from the database table Channel.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class ChannelRecord {
  public String id; // Unique channel ID
  public String name;
  public String description;
  public BigDecimal created;
  public String creatorId;
  public Long subscriptions;

  /** Empty constructor; for populating public fields from outside */
  public ChannelRecord() {}

  /**
   * Constructor.
   * @param record Arbitrary database record
   */
  public ChannelRecord(Record record) {
    this.id = record.getString("id");
    this.name = record.getString("name");
    this.description = record.getString("description");
    this.created = record.getBigDecimal("created");
    this.creatorId = record.getString("creator_id");
    this.subscriptions = record.getLong("subscriptions");
  }
}
