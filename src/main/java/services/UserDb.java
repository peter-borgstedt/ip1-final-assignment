package services;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import common.Crypto;
import datastructures.UserRecord;
import datastructures.UserRegistration;
import services.DbUtils.ConstraintException;

public class UserDb {
  private UserDb() {
    throw new InstantiationError("Forbidden instantiation");
  }

  public static String createUser(UserRegistration registration)
  throws ReflectiveOperationException, SQLException, ConstraintException, GeneralSecurityException {
    var query =
      "insert into users (id, email, password, forename, surname) " + 
      "values (?, ?, ?, ?, ?)";

    var secretKeyId = System.getProperty("PASSWORD_SECRET_KEY_ID");
    var encryptedPassword = Crypto.encrypt(secretKeyId, registration.password);

    var parameters = new Object[] {
        // "id" will be generated in DbUtils.updateWithId
        registration.email, // email
        encryptedPassword, // password
        registration.forename, // forename
        registration.surname
      }; // surname

    try {
      var db = new Db();
      return DbUtils.updateWithID(db, query, parameters);
    } catch (ConstraintException e) {
      var constraint = e.inheritance.getConstraint();

      if (constraint.equals("users_email_key")) { // email unique violation
        var message = "A user is already registered with the given email";
        throw new ConstraintException(e.inheritance, message);
      }
      throw e;
    }
  }

  public static void updateUser(String id, Map<String, Object> changeSet)
  throws SQLException, ReflectiveOperationException, GeneralSecurityException {
    var query = new StringBuilder("update users set ");
    var parameters = new ArrayList<Object>();

    if (changeSet.containsKey("password")) {
      var secretKeyId = System.getProperty("PASSWORD_SECRET_KEY_ID");
      changeSet.put("password", Crypto.encrypt(secretKeyId, String.class.cast(changeSet.get("password"))));
    }

    for (var change : changeSet.entrySet()) {
      query.append(String.format("%s = ?, ", change.getKey()));
      parameters.add(change.getValue());
    }

    // Remove the last ", " and replace it with "where id = ?"
    query.replace(query.length() - 2, query.length(), " where id = ?");
    parameters.add(id);

    var db = new Db();
    db.executeUpdate(query.toString(), parameters.toArray());
  }

  public static UserRecord getUser(String id)
  throws ReflectiveOperationException, SQLException, GeneralSecurityException {
    var query = new StringBuilder("select * from users where id = ?").toString();

    var db = new Db();
    var records = db.executeQuery(query, id);

    var firstRecord = records.stream().findFirst();
    if (!firstRecord.isPresent()) {
      return null; // No user found
    }

    var record = firstRecord.get();
    return new UserRecord(record);
  }

  /**
   * Get user by querying from the user email.
   * @param email Email of user
   * @return a database record of the user if any was found else null
   */
  public static UserRecord getUserByEmail(String email)
  throws ReflectiveOperationException, SQLException, GeneralSecurityException {
    var sql = new Db();

    var query = new StringBuilder("select * from users where email = ?").toString();
    var records = sql.executeQuery(query, email);

    var firstRecord = records.stream().findFirst();
    if (!firstRecord.isPresent()) {
      return null; // No user found
    }

    var record = firstRecord.get();
    return new UserRecord(record);
  }
}
