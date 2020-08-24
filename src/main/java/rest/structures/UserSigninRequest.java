package rest.structures;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * Request body for signing in with user credential.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class UserSigninRequest {
  public String email;
  public String password;
}