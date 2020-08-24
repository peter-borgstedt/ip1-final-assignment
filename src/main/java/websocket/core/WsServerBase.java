package websocket.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import websocket.Connection;
/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * A base implementation of a websocket service.
 * 
 * There is normally one instance per connection, the behavior has intentionally
 * changed so the WsConfiguration only uses one instance for all connections,
 * this gives a easier implementation to work with when a pool of connections
 * are being managed, all have to be synchronized and correcty so no concurrency
 * issues like obsolete data or dead locks occurs. See {@link WsConfiguration} for
 * details about the websocket configuration.
 *
 * Referencess:
 * https://www.baeldung.com/java-websockets
 * https://examples.javacodegeeks.com/enterprise-java/servlet/java-servlet-websocket-example/
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public abstract class WsServerBase {
  private static final Logger log = LogManager.getLogger(WsServerBase.class);

  // Id of session to Connection content
  protected static final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();
  protected static final HashMap<String, ScheduledExecutorService> pingExecutorServices = new HashMap<>();

  /**
   * When a connection has been established (after the handshake).
   * @param session Websocket session opened
   */
  @OnOpen
  public final void internalOpen(Session session) {
    try (WsRequestDataContext context = WsRequestDataContext.getCurrentInstance()) {
      log.info(String.format("Claims: %s", context.getProperties().get("claims")));
      log.info(String.format("Remote host: %s", context.getProperties().get("remoteHost")));
      log.info(String.format("Server host: %s", context.getProperties().get("serverHost")));

      var connection = new Connection(session, context);
      WsServerBase.connections.put(session.getId(), connection);

      // Start ping-pong (keep-alive) to connection
      var pingExecutorService = Executors.newScheduledThreadPool(1);
      pingExecutorService.scheduleAtFixedRate(() -> onPingMessage(session), 30, 30, TimeUnit.SECONDS);
      pingExecutorServices.put(session.getId(), pingExecutorService);

      onOpen(connection);
    } catch (Exception e) {
      log.error("Illegal access, rejecting session");
      e.printStackTrace();
      WsUtils.rejectSession(session);
    }
  }

  /**
   * Manual close of connection.
   * @param session Websocket connection being closed
   * @return removed Connection object for the session
   */
  protected Connection closeConnection (Session session) {
    if (session.isOpen()) {
      WsUtils.closeSession(session);
    }

    // Stop ping-pong (keep-alive) to connection
    var pingExecutorService = pingExecutorServices.remove(session.getId());
    pingExecutorService.shutdownNow();

    return connections.remove(session.getId());
  } 

  /**
   * When a connection is closed.
   * @param session Websocket session being closed
   */
  @OnClose
  final public void internalClose(Session session) {
    var connection = closeConnection(session);

    // Will be null if no connection was created,
    // which will happened if the JWT signature (credential)
    // is invalid, session will then be closed for these
    if (connection != null) {
      onClose(connection);
    }
  }

  /**
   * When an error occur.
   * @param e Exception thrown
   */
  @OnError
  public void internalError(Throwable e) {
    log.error(String.format("Caught an error = %s", e), e);
  }

  /**
   * On sending a Ping (keep-alive) message to existing session.
   * @param session Websocket session
   */
  private void onPingMessage(Session session) {
    try {
      log.info(String.format("Ping message sent to session: %s", session.getId()));
      session.getBasicRemote().sendPing(ByteBuffer.wrap("ping".getBytes()));
    } catch (IOException e) {
      log.error(String.format("Error occurred while sending ping to session: %s", session.getId()), e);

      // Ping failed to be sent, close connection (if not already)...
      WsUtils.closeSession(session);
      internalClose(session);
    }
  }

  /**
   * On received Pong respond message from existing session.
   * @param session Websocket session
   * @param message Websocket session
   */
  @OnMessage
  public void onPongMessage(Session session, PongMessage message) {
    log.info(String.format("Pong message received from session: %s", session.getId()));
  }

  /**
   * Abstract method to be overidden by extending class with a
   * specific implementation of usage.
   * @param session Websocket connection opened
   */
  public abstract void onOpen(Connection session);

  /**
   * Abstract method to be overidden by extending class with a
   * specific implementation of usage.
   * @param session Websocket connection being closed
   */
  public abstract void onClose(Connection session);
}
