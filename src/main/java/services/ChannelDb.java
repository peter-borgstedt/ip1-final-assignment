package services;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.Id;
import datastructures.ChannelRecord;
import datastructures.Image;
import datastructures.MessageRecord;
import datastructures.StoredMessages;
import services.DbUtils.ConstraintException;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * References:
 * https://stackoverflow.com/a/1360652
 * https://stackoverflow.com/a/30968827
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class ChannelDb extends Db {
  private static final Logger log = LogManager.getLogger(ChannelDb.class);

  /**
   * Creates a channel.
   * 
   * @param channel   Channel details
   * @param creatorId ID of the user creating the channel
   * @return created channel details
   * @throws SQLException
   * @throws ReflectiveOperationException
   * @throws ConstraintException
   */
  public static ChannelRecord createChannel(String name, String description, String creatorId)
  throws ReflectiveOperationException, SQLException, ConstraintException {
    var query =
      "insert into channels (id, name, description, created, creator_id) " +
      "values (?, ?, ?, ?, ?)";

    var created = Instant.now().toEpochMilli();

    // "id" will be generated in DbUtils.updateWithId
    var parameters = new Object[] { name, description, created, creatorId };

    try {
      var db = new Db();
      var id = DbUtils.updateWithID(db, query, parameters);

      var channel = new ChannelRecord();
      channel.id = id;
      channel.name = name;
      channel.description = description;
      channel.created = BigDecimal.valueOf(created);
      channel.creatorId = creatorId;
      // Only the creator is subscribing on it when newly created
      channel.subscriptions = 1L;

      return channel;
    } catch (ConstraintException e) {
      var constraint = e.inheritance.getConstraint();

      if (constraint.equals("channel_name_key")) { // channel unique violation
        var message = "A channel has already been registered with the given name";
        throw new ConstraintException(e.inheritance, message);
      }

      throw e;
    }
  }

  /**
   * Deletes a channel, its subscriptions and messages.
   * @param channelId ID of the channel
   */
  public static void deleteChannel(String channelId)
  throws ReflectiveOperationException, SQLException {
    var db = new Db();

    // Remove subscriptions on channel
    var querySubscriptions = "delete from subscriptions where channel_id = ?";
    var parametersSubscriptions = new String[] { channelId };
    db.executeUpdate(querySubscriptions, parametersSubscriptions);

    // Remove messages for channel
    var queryMessages = "delete from messages where channel_id = ?";
    var parametersChannel = new String[] { channelId };
    db.executeUpdate(queryMessages, parametersChannel);

    // Remove channel
    var queryChannels = "delete from channels where id = ?";
    var parametersChannels = new String[] { channelId };
    db.executeUpdate(queryChannels, parametersChannels);
  }

  /**
   * Get channel with a given ID.
   * @param id ID of the channel
   * @return channel details
   */
  public static ChannelRecord getChannel(String id)
  throws ReflectiveOperationException, SQLException {
    var query =
      "select c.*, count(subscr.channel_id) as subscriptions " +
      "from channels c " +
      "left join (select channel_id from subscriptions) as subscr on c.id = subscr.channel_id " +
      "where c.id = ? " +
      "group by c.id, c.description, c.created, subscr.channel_id ";

    var db = new Db();
    var records = db.executeQuery(query, id);

    var firstRecord = records.stream().findFirst();
    if (firstRecord.isPresent()) {
      return new ChannelRecord(firstRecord.get());
    }
    return null;
  }

    /**
   * Get all channels
   * @return List of channels
   */
  public static List<ChannelRecord> getChannels()
  throws ReflectiveOperationException, SQLException {
    var query =
      "select c.*, count(subscr.channel_id) as subscriptions " +
      "from channels c " +
      "left join (select channel_id from subscriptions) as subscr on c.id = subscr.channel_id " +
      "group by c.id, c.description, c.created, subscr.channel_id";

    var db = new Db();
    var records = db.executeQuery(query);

    return records.stream()
      .map(record -> new ChannelRecord(record))
      .collect(Collectors.toList());
  }

  /**
   * Get subscribing channels of a user.
   * @param userId ID of the suser with channel subscriptions 
   * @return List of channels
   */
  public static List<ChannelRecord> getChannels(String userId)
  throws ReflectiveOperationException, SQLException {
    var query =
      "select c.*, count(subscr.channel_id) as subscriptions " +
      "from subscriptions s " +
      "join channels c on c.id = s.channel_id and s.user_id = ? " +
      "join (select channel_id from subscriptions) as subscr on c.id = subscr.channel_id " +
      "group by c.id, c.description, c.created, subscr.channel_id";

    var db = new Db();
    var records = db.executeQuery(query, userId);

    return records.stream()
      .map(record -> new ChannelRecord(record))
      .collect(Collectors.toList());
  }

  /**
   * Subscribe by adding channel subscription for user.
   * @param userId ID of the user 
   * @param channelId ID of the channel
   */
  public static void subscribeChannel(String userId, String channelId)
  throws ReflectiveOperationException, SQLException {
    var query = "insert into subscriptions (user_id, channel_id) values (?, ?)";
    var parameters = new String[] { userId, channelId };

    var db = new Db();
    db.executeUpdate(query, parameters);
  }

  /**
   * Unsubscribe by removing channel subscription of user.
   * @param userId ID of the user 
   * @param channelId ID of the channel
   */
  public static void unsubscribeChannel(String userId, String channelId)
  throws ReflectiveOperationException, SQLException {
    var query = "delete from subscriptions where user_id = ? and channel_id = ?";
    var parameters = new String[] { userId, channelId };

    var db = new Db();
    db.executeUpdate(query, parameters);
  }

  /**
   * Adds message to an associated channel.
   * @param userId ID of the user 
   * @param channelId ID of the channel
   * @param data Message
   */
  public static MessageRecord addMessage(String userId, String channelId, Object data)
  throws ReflectiveOperationException, SQLException, IOException {
    var id = Id.generate();
    var created = Instant.now().toEpochMilli();

    var query = "insert into messages (id, channel_id, user_id, created, data) values  (?, ?, ?, ?, ?)";
    var parameters = new Object[] { id, channelId, userId, created, DbUtils.serialize(data) };

    var db = new Db();
    db.executeUpdate(query, parameters);

    var message = new MessageRecord();
    message.id = id;
    message.channelId = channelId;
    message.userId = userId;
    message.index = -1; // Not needed (no pagination)
    message.created = BigDecimal.valueOf(created);
    message.type = data instanceof Image ? "image" : "text";
    message.data = data;
    return message;
  }

  /**
   * Removes a message associated by a user and channel.
   * @param messageId ID of the message 
   * @param channelId ID of the channel
   * @param userId ID of the the user
   */
  public static int removeMessage(String messageId, String channelId, String userId)
  throws ReflectiveOperationException, SQLException {
    var query = "delete from messages where id = ? and channel_id = ? and user_id = ?";
    var parameters = new String[] { messageId, channelId, userId };

    var db = new Db();
    return db.executeUpdate(query, parameters);
  }

  /**
   * Get messages with pagination for a channel.
   * @param channelId ID of the channel
   * @param from Query from index
   * @param limit Amount of records to retrieve
   * @return the records and whether the query has reached the end
   */
  public static StoredMessages getMessages(String channelId, Integer from, int limit)
  throws ReflectiveOperationException, SQLException, IOException {
    var query = "select * from messages where channel_id = ? ";
    if (from != null) {
      query += "and idx < ? ";
    }
    query += "order by created desc limit ?";

    // Add 1 for checking if there are more data existing
    var limitWithOffset = limit + 1;

    var parameters = from == null ? new Object[] { channelId, limitWithOffset } : new Object[] { channelId, from, limitWithOffset };

    var db = new Db();
    var records = db.executeQuery(query, parameters);

    var messages = new StoredMessages();
    messages.records = new ArrayList<>();
    messages.hasMore = records.size() > limit;

    log.debug(String.format("More records exists: %s > %s = %s", records.size(), limit, records.size() > limit));

    for (int i = 0; i < Math.min(records.size(), limit); i++) {
      var message = new MessageRecord(records.get(i));
      messages.records.add(message);
    }
    return messages;
  }
}
