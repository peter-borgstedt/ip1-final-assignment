package websocket;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.websocket.CloseReason;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.ActionDataCaster;
import common.Json;
import datastructures.Image;
import datastructures.ImageData;
import datastructures.MessageAction;
import datastructures.MessageRecord;
import datastructures.Text;
import datastructures.UserInfo;
import datastructures.WsActionRequest;
import datastructures.WsActionResponse;
import services.ChannelDb;
import services.S3;
import services.UserDb;
import websocket.core.WsConfiguration;
import websocket.core.WsServerBase;
import websocket.core.WsUtils;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Implementation of the websocket service for the chat.
 *
 * Handles incoming messages that consist of an action; which can be a message,
 * an image or some kind of update on perhapse a user, basically anything that
 * needs to be broadcasted and updated on the client side real time.
 * 
 * A connection can be associated with one or several channels, anything
 * broadcasted for these channels will be broadcasted for connections associated
 * with them.
 * 
 * References:
 * https://abhishek-gupta.gitbook.io/java-websocket-api-handbook/lifecycle_and_concurrency_semantics
 * https://abhirockzz.wordpress.com/2017/02/14/websocket-endpoint-as-singleton-ejb/
 * 
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
@Singleton
@ServerEndpoint(value = "/ws", configurator = WsConfiguration.class)
public class WsServer extends WsServerBase {
  private static final Logger log = LogManager.getLogger(WsServer.class);

  // Heavily used to test the expression:
  // https://www.freeformatter.com/java-regex-tester.html
  private static final Pattern BASE64_META_PATTERN = Pattern.compile("data:image/(?<format>\\w+)(?:\\+\\w+)?;base64");

  private static final ConcurrentHashMap<String, byte[]> transfers = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, Set<Connection>> channelIdToConnections = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, Connection> userIdToConnection = new ConcurrentHashMap<>();
  /**
   * A new connection has been established.
   * @param connection Websocket connection
   */
  @Override
  public void onOpen(Connection connection) {
    WsServer.userIdToConnection.put(connection.getUserID(), connection);

    // Log connection
    var id = connection.getId();
    var remoteAddr = connection.getRemoteAddr();
    var userId = connection.getUserID();
    var userEmail = connection.getUserEmail();

    log.info(String.format("Connection opened = { id: %s, remoteAddress: %s, userId: %s, userEmail: %s }", id, remoteAddr, userId, userEmail));

    // Set up existing subscriptions for connection
    try {
      var subscribedChannels = ChannelDb.getChannels(userId);
      for (var channel : subscribedChannels) {
        WsServer.channelIdToConnections.computeIfAbsent(channel.id, k -> new HashSet<>()).add(connection);
      }
    } catch (Exception e) {
      log.error(String.format("Could not setup subscriptions for connection: %s", connection.getId()));
      e.printStackTrace();
    }
  }

  /**
   * An existing connection has been closed.
   * @param connection Websocket connection
   */
  @Override
  public void onClose(Connection connection) {
    WsServer.userIdToConnection.remove(connection.getUserID(), connection);

    for (var value : WsServer.channelIdToConnections.values()) {
      value.remove(connection); // Remove connection for channels
    }

    var id = connection.getId();
    var remoteAddr = connection.getRemoteAddr();
    var userId = connection.getUserID();
    var userEmail = connection.getUserEmail();
    log.info(String.format("Connection closed = { id: %s, remoteAddress: %s, userId: %s, userEmail: %s }", id, remoteAddr, userId, userEmail));
  }

  /**
   * Process binary stream by accumulating previous data with new until all data
   * has been transferred.
   * 
   * @param newData New byte array of data
   * @param finish  If all data has been transferred
   * @param session Websocket connection
   */
  private void processBinaryMessage(byte[] newData, boolean finish, Session session) {
    // One connection cannot do concurrent streaming, so this is probably
    // unnecessary thread protection
    // also each instance of this class is attached to a Session so it will not
    // intervene with any other,
    // instead just store a data array container as a private variable and then when
    // finish clear it
    if (WsServer.transfers.containsKey(session.getId())) {
      var existingData = WsServer.transfers.get(session.getId());
      var concatenatedData = Arrays.copyOf(existingData, existingData.length + newData.length);

      // Append to previous transfer data
      System.arraycopy(newData, 0, concatenatedData, existingData.length, newData.length);

      WsServer.transfers.put(session.getId(), concatenatedData);
    } else {
      WsServer.transfers.put(session.getId(), newData);
    }
  }

  private String storeImageByteArray(Connection connection, ImageData image)
  throws IOException {
    var checksum = getCheckSum(image.data);
    var catalinaBaseDir = System.getProperty("catalina.base");
    var directory = System.getProperty("IMAGES");

    var filePath = String.format("%s/%s/%s.%s", catalinaBaseDir, directory, checksum, image.format);
    log.info(String.format("Attempting to store image at: %s", filePath));

    var file = new File(filePath);
    // As the filename is being generated using a checksum we only write if the file does not
    // exist since earlier, if it exist the resuse that url 
    if (!file.exists()) {
      // If create upload directory on server if not existing
      file.getParentFile().mkdirs();

      // Write file locally on Tomcat (temporary storage)
      Files.write(file.toPath(), image.data);

      log.info(String.format("Successfully written image to: %s", file));

      // Store in AWS S3 (simple service)
      // Add file also ınto a "persistent storage" and broadcast the image url to this storage
      // instead of using the Tomcat to host files, which will have the leverage of minimizing the amount
      // of traffic going to the server and save space on the disc
      var s3 = new S3(System.getProperty("AWS_S3_UPLOAD"));
      s3.put(String.format("images/%s.%s", checksum, image.format), file);

      log.info("Image successfully stored");
    } else {
      log.info("Image already stored");
    }

    log.info(String.format("Image located in server at: %s", file.getAbsolutePath()));

    var region = System.getProperty("AWS_REGION");
    var bucket = System.getProperty("AWS_S3_UPLOAD");
    var imageUrl = String.format("https://%s.s3-%s.amazonaws.com/images/%s.%s", bucket, region, checksum, image.format);

    log.info(String.format("Image is obtainable from S3 at: %s", imageUrl));
    return imageUrl;
  }

  /**
   * Get file checksum for when storing, if file already exists do not store it.
   * https://stackoverflow.com/a/34448106
   * https://stackoverflow.com/a/26231444
   * @param bytes Bytes to calculate checksum on
   * @return checksum as a hash
   */
  private String getCheckSum(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return DatatypeConverter.printHexBinary(md.digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Parses and transforms a base64 string into a binary image
   * @param imageData base64 string containing an image
   * @return meta and binary data of image
   */
  private ImageData readBase64Image(String imageBase64String) {
    var imageBase64Array = imageBase64String.split(",");
    var imageMeta = imageBase64Array[0];
    var imageData = imageBase64Array[1].getBytes(StandardCharsets.UTF_8);

    log.info(String.format("Base64 metadata: %s", imageMeta));

    // The base64 meta description will look something like "data:image/jpeg;base64",
    // or even "data:image/svg+xml;base64" depending on format, use a regular expression
    // to retrieve this using a named capture group
    var matcher = WsServer.BASE64_META_PATTERN.matcher(imageMeta);
    var format = matcher.find() ? matcher.group("format") : "jpeg"; // Default to "jpeg"

    return new ImageData(format.strip(), Base64.getDecoder().decode(imageData));
  }

  /**
   * Processes a message action which is either of plain text or with an attached
   * image.
   * @param connection Websocket connection
   * @param message Message containing text and/or image
   */
  private void incomingMessageAction(Connection connection, MessageAction message)
  throws ReflectiveOperationException, SQLException, IOException {
    var userId = connection.getUserID();
    log.info(String.format("Got message from %s -> { type: %s, channelId: %s }", userId, message.type, message.channelId));

    switch (message.type) {
      // Message with plain text and/or image
      case "image": {
        // Store image
        var imageBase64 = message.data.get("image");
        var imageData = readBase64Image(imageBase64);
        var imageUrl = storeImageByteArray(connection, imageData);

        // Send image message to all connections subscribing to channel
        var data = new Image(message.data.get("text"), imageUrl);
        var postedMessage = ChannelDb.addMessage(userId, message.channelId, data);

        broadcastMessage(connection, postedMessage);
        break;
      }
      // Message with plain text
      case "text": {
        // Send text message to all connections subscribing to channel
        var data = new Text(message.data.get("text"));
        var postedMessage = ChannelDb.addMessage(userId, message.channelId, data);

        broadcastMessage(connection, postedMessage);
        break;
      }
      default: {
        // Just print a stack trace do not throw and break stuff (as it will not be
        // catched)
        var exception = new IllegalArgumentException(String.format("Unsupported message type %s", message.type));
        exception.printStackTrace();
      }
    }
  }

  /**
   * Broadcasts a message action, containing a message with plain text or/and
   * image. The message is broadcaste to associated channel it was created in.
   * @param connection Websocket connection
   * @param message Message (that has been stored in the database)
   */
  private void broadcastMessage(Connection connection, MessageRecord message) {
    log.info(String.format("Broadcasting (%s) to %s with text = %s",
        message.type, message.channelId, message.data));
    var connections = WsServer.channelIdToConnections.get(message.channelId);

    if (connections == null || connections.isEmpty()) {
      log.info(String.format("No active connections subscribing on channel %s", message.channelId));
    } else {
      var response = new WsActionResponse();
      response.type = "message";
      response.data = message;

      broadcast(connections, response);
    }
  }

  /**
   * A user profile has been updated and needs to be broadcasted to connections
   * associated with channels that contains this user (connection).
   * 
   * @param connection Websocket connection
   * @param action Action with content to be processed and broadcasted
   */
  private void incomingProfileChangeAction(Connection connection, WsActionRequest action)
  throws SQLException, ReflectiveOperationException, GeneralSecurityException, IOException {
    var data = action.data;

    var changeSet = new HashMap<String, Object>(data);
    changeSet.remove("profileImageData");

    if (data.containsKey("profileImageData")) {
      var profileImageData = data.get("profileImageData");
      if (profileImageData == null) {
        // Profile image has been removed by the user
        changeSet.put("profile_image_url", null);
      } else {
        // Store image
        var imageBase64 = String.class.cast(data.get("profileImageData"));
        var imageData = readBase64Image(imageBase64);
        var imageUrl = storeImageByteArray(connection, imageData);

        // Add profile image url to changes
        changeSet.put("profile_image_url", imageUrl);
      }

      log.info(String.format("profile_image_url: %s", changeSet.get("profile_image_url")));
    }

    // Update changes in database
    UserDb.updateUser(connection.getUserID(), changeSet);
    var userInfo = new UserInfo(UserDb.getUser(connection.getUserID()));

    // Get connections contained in same channel as user being changed 
    Set<Connection> connections = channelIdToConnections.entrySet().stream()
      .filter((entry) -> entry.getValue().contains(connection))
      .map((entry) -> entry.getValue())
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());

    // Create response object
    var response = new WsActionResponse();
      response.type = "profile-changed";
      response.data = userInfo;

    // Broadcast changes to associated connections
    broadcast(connections, response);
  }

  /**
   * Delete a channel indicating a removal of all connections associated with the channel.
   * @param connection Websocket connection
   * @param action Action with content to be processed and broadcasted
   */
  private void incomingMessageDelete(Connection connection, WsActionRequest action)
  throws ReflectiveOperationException, SQLException {
    var data = action.data;

    var messageId = String.class.cast(data.get("id"));
    var channelId = String.class.cast(data.get("channelId"));
    var userId = connection.getUserID();

    var affected = ChannelDb.removeMessage(messageId, channelId, userId);
    if (affected == 1) {
      var response = new WsActionResponse();
      response.type = "message-deleted";
      response.data = data;

      var connections = channelIdToConnections.get(channelId);
      broadcast(connections, response);
    }
  }

  /**
   * Deleting a channel indicates removal of all connections associated with the channel,
   * the subscriptions, the messages and the channel itself in the database.
   * @param connection Websocket connection
   * @param action Action with content to be processed and broadcasted
   */
  private void incomingChannelDelete(Connection connection, WsActionRequest action)
  throws ReflectiveOperationException, SQLException {
    var data = action.data;
    var channelId = String.class.cast(data.get("id"));

    var userId = connection.getUserID();

    var channel = ChannelDb.getChannel(channelId);
    log.info(String.format("Is channel creator: %s == %s = %s", channel.creatorId, userId, channel.creatorId.equals(userId)));

    // Check if user is the creator, only the user that has created the channel my delete it
    if (channel.creatorId.equals(userId)) {
      // Remove channel, subscriptions and messages in this channel
      ChannelDb.deleteChannel(channelId);

      // Remove and get connections for channel
      var connections = WsServer.channelIdToConnections.remove(channelId);
      
      // Broadcast changes to associated connections
      var response = new WsActionResponse();
      response.type = "channel-deleted";
      response.data = channelId;

      // Broad cast to associated users; subscribed to channel
      broadcast(connections, response);
    }
  }

  /**
   * Unsubscription on a channel indicates adding the connection to the channel
   * connection pool and adding subscription for user on channel in the database.
   * @param connection Websocket connection
   * @param action Action with content to be processed and broadcasted
   */
  private void incomingChannelSubscribe(Connection connection, WsActionRequest action)
  throws ReflectiveOperationException, SQLException {
    var data = action.data;
    var channelId = String.class.cast(data.get("id"));
    var userId = connection.getUserID();

    // Set the creating user as subscriber on channel
    ChannelDb.subscribeChannel(userId, channelId);

    // Add connection to channel
    WsServer.channelIdToConnections.computeIfAbsent(channelId, k -> new HashSet<>()).add(connection);

    // Get all connections for channel
    var connections = WsServer.channelIdToConnections.get(channelId);

    // Get subscribed channel
    var subscribedChannel = ChannelDb.getChannel(channelId);

    // Format data for outgoing response
    var subscribedChannelData = Json.objectToMap(subscribedChannel);
    subscribedChannelData.put("userId", userId);

    var response = new WsActionResponse();
    response.type = "channel-subscribed";
    response.data = subscribedChannelData;

    // Broadcast changes to associated connections
    broadcast(connections, response);
  }

  /**
   * Unsubscription on a channel indicates removing connection from the channel
   * connection pool and removing subscription for user from channel in the database.
   * @param connection Websocket connection
   * @param action Action with content to be processed and broadcasted
   */
  public void incomingChannelUnsubscribe(Connection connection, WsActionRequest action)
  throws ReflectiveOperationException, SQLException {
    var data = action.data;
    var channelId = String.class.cast(data.get("id"));
    var userId = connection.getUserID();

    // Include the ID of the user unsubscribing on the channel
    data.put("userId", userId);

    // Set the creating user as subscriber on channel
    ChannelDb.unsubscribeChannel(userId, channelId);

    // Get all connections for channel
    var connections = WsServer.channelIdToConnections.get(channelId);

    var response = new WsActionResponse();
    response.type = "channel-unsubscribed";
    response.data = data;

    // Broadcast changes to associated connections (including connection being removed)
    broadcast(connections, response);

    // Remove connection from channel (after broadcast)
    connections.remove(connection);
  }

  /**
   * Processes incoming events which will be processed and then broadcasted
   * to associated connections.
   * @param connection Websocket connection
   * @param action Action with content to be processed and broadcasted
   */
  private void processAction(Connection connection, WsActionRequest action)
  throws ReflectiveOperationException, SQLException, IOException, GeneralSecurityException {
    switch (action.type) {
      // A message has been added (to channel)
      case "message": {
        var message = ActionDataCaster.getMessage(action.data);
        incomingMessageAction(connection, message);
        break;
      }
      // A message has been removed (from a channel)
      case "message-delete": {
        incomingMessageDelete(connection, action);
        break;
      }
       // A channel as been removed
      case "channel-delete": {
        incomingChannelDelete(connection, action);
        break;
      }
      // A channel as been subscribed
      case "channel-subscribe": {
        incomingChannelSubscribe(connection, action);
        break;
      }
      // A channel as been removed
      case "channel-unsubscribe": {
        incomingChannelUnsubscribe(connection, action);
        break;
      }
      // A user profile has been updated
      case "profile-update": {
        incomingProfileChangeAction(connection, action);
        break;
      }
      default: {
        throw new IllegalArgumentException(String.format("Unsupported action %s", action.type));
      }
    }
  }

  /**
   * On recieving binary message which will be streamed and collected then
   * transformed into its represented data.
   *
   * Better for handling large messages containing, especially when it comes to images,
   * all messages will be streamed using this method.
   *
   * @param newData All or chunk of data
   * @param finish If data stream has finished
   * @param session Websocket session
   */
  @OnMessage(maxMessageSize = 1024000)
  public void onBinaryMessage(byte[] newData, boolean finish, Session session) {
    log.info(String.format("From: %s -> Chunk: %s -> Finish: %s", session.getId(), newData.length, finish));
    processBinaryMessage(newData, finish, session);

    if (finish) {
      // Remove and return previous associated value
      var bytes = WsServer.transfers.remove(session.getId());
      var connection = WsServerBase.connections.get(session.getId());

      try {
        // Transform into an action object
        var action = Json.parse(bytes, WsActionRequest.class);
        // Process action object
        processAction(connection, action);
      } catch (JsonProcessingException e) {
        System.err.println("[WsServer::onBinaryMessage] could not parse message, ignoring...");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Broadcast a response action to associated connections with any related data.
   * @param connections Websocket connections to retrieve data
   * @param response Response data to be sent
   */
  private synchronized void broadcast(Set<Connection> connections, WsActionResponse response) {
    log.info(String.format("Sending to %s connections", connections.size()));

    for (var connection : connections) {
      if(connection.getSession() == null) { // Should never happen (as we cleanup, but add it if any bugs)
        // Just print out a stacktrace, do not throw...
        new RuntimeException("Found connection with no active session").printStackTrace();
        connections.remove(connection); // Remove from collection so no further errors occurr
        continue; // Skip and continue
      }

      log.debug(String.format("Connection: %s", connection));
      log.debug(String.format("Session is: %s", connection.getSession()));
      log.debug(String.format("Session properties: %s", connection.getProperties()));
      log.debug(String.format("Session claims: %s", connection.getProperties().get("claims")));

      try {
        log.info(String.format("%s -> %s: ", connection.getUserID(), connection.getId()));
        connection.getSession().getBasicRemote().sendText(Json.stringify(response));

        log.info("Successfully sent");
      } catch (IOException e) { // Close any problematic connections, they may also already be closed
        WsUtils.closeSession(connection.getSession(), CloseReason.CloseCodes.UNEXPECTED_CONDITION);
      }
    }
  }

  /**
   * Subscribe a connection (the creator) for newly created channel.
   * This is called when a channel is created, initiation of all subscriptions for
   * a connection is done when a websocket connection is opened.
   *
   * New channels are created with the REST api as there is no need to broadcast
   * any messages when a channel has been created.
   *
   * @param userId User ID associated with a connection
   * @param channelId Channel ID assoicated with user and connection
   */
  public void subscribeNewChannel(String userId, String channelId) {
    var connection = WsServer.userIdToConnection.get(userId);
    if (connection == null) {
      new RuntimeException("Could not find an active connection for user with ID: " + userId).printStackTrace();
    } else {
      WsServer.channelIdToConnections.computeIfAbsent(channelId, k -> new HashSet<>()).add(connection);
    }
  }
}
