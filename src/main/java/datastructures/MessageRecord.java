package datastructures;

import static services.DbUtils.deserialize;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a record from the database table Message.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class MessageRecord {
  public String id;
  public String userId;
  public String channelId;
  public Integer index;
  public String type;
  public Object data;
  public BigDecimal created;

  /** Empty constructor; for populating public fields from outside */
  public MessageRecord() {}

  /**
   * Constructor.
   * @param record Arbitrary database record
   */
  public MessageRecord(Record record) throws IOException, ReflectiveOperationException {
    this.index = record.getInteger("idx");
    this.id = record.getString("id");
    this.channelId = record.getString("channel_id");
    this.userId = record.getString("user_id");
    this.created = record.getBigDecimal("created");
    this.data = deserialize(record.getByteArray("data"), Object.class);
    this.type = data instanceof Image ? "image" : "text";
  }
}
