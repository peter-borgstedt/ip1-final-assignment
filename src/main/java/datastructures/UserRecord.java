package datastructures;

import java.security.GeneralSecurityException;

import common.Crypto;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a record from the database table User.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class UserRecord {
  public String id;
  public String forename;
  public String surname;
  public String email;
  public String password;
  public String profileImageUrl;

  /**
   * Constructor.
   * @param record Arbitrary database record
   */
  public UserRecord(Record record) throws GeneralSecurityException {
    var secretKeyId = System.getProperty("PASSWORD_SECRET_KEY_ID");
    var encryptedPassword = record.getString("password");
    var decryptedPassword = Crypto.decrypt(secretKeyId, encryptedPassword);

    this.id = record.getString("id");
    this.email = record.getString("email");
    this.password = decryptedPassword;
    this.forename = record.getString("forename");
    this.surname = record.getString("surname");
    this.profileImageUrl = record.getString("profile_image_url");
  }
}
