package datastructures;

import java.security.GeneralSecurityException;

import rest.structures.UserRegistrationRequest;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a registration. This is a bit redundant to the
 * {@link rest.structures.UserRegistrationRequest}, but is so intentionally
 * to keep the class context out from logic it is not directly part of.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class UserRegistration {
  public String forename;
  public String surname;
  public String email;
  public String password;

  /**
   * Constructor.
   * @param record Arbitrary database record
   */
  public UserRegistration(UserRegistrationRequest data) throws GeneralSecurityException {
    this.email = data.email;
    this.password = data.password;
    this.forename = data.forename;
    this.surname = data.surname;
  }
}
