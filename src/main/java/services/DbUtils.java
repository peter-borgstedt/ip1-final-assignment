package services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import common.Id;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * A implementation that wraps a SQL connection to an PostgreSQL with
 * help methods for queries and manipulations (DQL and DML).
 * 
 * Using prepared statement as this will be faster for continuous inserts and
 * more important its prevents sql injection.
 * 
 * References:
 * https://stackoverflow.com/questions/8263371/how-can-prepared-statements-protect-from-sql-injection-attacks/8265319
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class DbUtils {
  private static final Logger log = LogManager.getLogger(DbUtils.class);

  DbUtils() {
    throw new InstantiationError("Forbidden instantiation");
  }

  /**
   * Serialize object.
   * Used when storing data as "bytea" (blob) in PostgreSQL.
   * @param obj Object to be serialized
   */
  public static byte[] serialize(Object obj) throws IOException {
    try (
      var out = new ByteArrayOutputStream();
      var os = new ObjectOutputStream(out);
    ) {
      os.writeObject(obj);
      return out.toByteArray();
    } catch (IOException e) {
      log.error(String.format("Failed to serialize object: %s", obj));
      throw e;
    }
  }

  /**
   * Deserialize object (and cast to class).
   * Used when querying data stored as "bytea" (blob) in PostgreSQL.
   * @param data Data to be deserialized into an object
   * @param clazz Class object should be casted into 
   */
  public static <T> T deserialize(byte[] data, Class<T> clazz)
  throws IOException, ReflectiveOperationException {
    try (
      var in = new ByteArrayInputStream(data);
      var is = new ObjectInputStream(in);
    ) {
      return clazz.cast(is.readObject());
    } catch (IOException e) {
      log.error(String.format("Failed to serialize byte array: %s", data));
      throw e;
    }
  }

  /**
   * A custom exception when primary key is violated during DDL.
   */
  private static class PrimaryKeyConstraintException extends Exception {
    private static final long serialVersionUID = 1L;

    private PrimaryKeyConstraintException(String message) {
      super(message);
    }
  }

  /** A custom exception when constraints are violated during DDL. */
  public static class ConstraintException extends Exception {
    private static final long serialVersionUID = 1L;
    public final ServerErrorMessage inheritance;

    public ConstraintException(ServerErrorMessage inheritance) {
      this(inheritance, inheritance.getMessage());
    }

    public ConstraintException(ServerErrorMessage inheritance, String message) {
      super(message);
      this.inheritance = inheritance;
    }
  }

  /**
   * Executes update and handles unique key violation. 
   * @param query Query to be run
   * @param parameters Parameters used in query
   */
  private static void updateUsingID(Db db, String query, Object ... parameters)
  throws ReflectiveOperationException, SQLException, ConstraintException, PrimaryKeyConstraintException {
    try {
      db.executeUpdate(query, parameters);
    } catch (PSQLException e) {
      var error = e.getServerErrorMessage();
      var state = e.getSQLState();
      log.error(String.format("{ state: %s, error: %s }", error, state));

      // https://www.postgresql.org/docs/12/errcodes-appendix.html
      if (state.equals("23505") && error != null && error.getConstraint() != null) { // unique_violation
        var constraint = error.getConstraint();
        if (constraint.endsWith("pkey")) { // primary key violation
          throw new PrimaryKeyConstraintException(e.getMessage());
        }
        throw new ConstraintException(error);
      }
      throw e;
    }
  }

  /**
   * Executes update but with the field ID populated with a unique generated
   * string.
   * @param query Query to be run
   * @param parameters Parameters used in query
   * @return generated id
   */
  protected static String updateWithID(Db db, String query, Object... parameters)
  throws ReflectiveOperationException, SQLException, ConstraintException {
    var tries = 0;
    while (tries <= 5) {
      var id = Id.generate(); // Generate ID from current time

      try {
        log.debug(String.format("Generated ID -> %s", id));

        var params = new ArrayList<>();
        params.add(id);
        params.addAll(Arrays.asList(parameters));

        updateUsingID(db, query, params.toArray());

        return id; // Return the generated ID (PK)
      } catch (PrimaryKeyConstraintException e) {
        // In the rare case that the key already exists generate a new and retry...
        // It is a bit "overkill" because this will probably never happen 
        log.error(String.format("Violated constraint using ID: %s - Retrying with new ID (%s of %s)", id, ++tries, 5));
      }
    }
    throw new SQLException("Could not create unique ID for user");
  }
}
