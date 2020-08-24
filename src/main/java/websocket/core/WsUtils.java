package websocket.core;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 * 
 * Static methods for handling websocket connections.
 *
 * Referencess:
 * https://github.com/eclipse-ee4j/websocket-api/issues/235
 * https://stackoverflow.com/a/17994303
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class WsUtils {
  private static final Logger log = LogManager.getLogger(WsUtils.class);

  public static final CloseReason CloseReasonForbidden = new CloseReason(() -> 4404, "Forbidden");

  private WsUtils() {
    throw new InstantiationError("Forbidden instantiation");
  }

  public static void rejectSession(Session session) {
    try {
      session.close(WsUtils.CloseReasonForbidden);
    } catch (IOException e) {
      // Do nothing but log, the connection may already be closed
      log.warn(String.format("Could not close session: %s", session.getId()));
    }
  }
 
  public static void closeSession(Session session) {
    closeSession(session, null);
  }

  public static void closeSession(Session session, CloseCode code) {
    closeSession(session, code, null);
  }

  public static void closeSession(Session session, CloseCode code, String reason) {
    try {
      if (code == null) {
        session.close();
      } else {
        session.close(new CloseReason(code, reason)); // Code is mandatory
      }
    } catch (IOException e) {
      // Do nothing but log, the connection may already be closed
      log.warn(String.format("Could not close session: %s", session.getId()));
    }
  }
}