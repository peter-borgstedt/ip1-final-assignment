package services;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * A service creating (signing) and parsing (reading) json web tokens.
 *
 * References:
 * https://www.programcreek.com/java-api-examples/?api=io.jsonwebtoken.Claims
 * https://stackoverflow.com/a/37635391
 * https://tools.ietf.org/html/rfc7519 (4. JWT Claims)
 * https://www.journaldev.com/1377/java-singleton-design-pattern-best-practices-examples
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class TokenService {
  private static final Logger log = LogManager.getLogger(TokenService.class);
  private static TokenService instance;

  public static final String AUTHENTICATION_SCHEME = "Bearer";

  private byte[] encodedKey;

  private TokenService() {
    // Only allow instantiation private
  }

  /* Get singleton instance */
  public static TokenService getInstance() {
    if (instance == null) { // Double locking
      instance = new TokenService();
    }
    return instance;
  }

  /**
   * Get the encoded key.
   * 
   * @return encoded key as bytes
   * @throws NoSuchAlgorithmException
   */
  private byte[] getEncodedKey()
  throws NoSuchAlgorithmException {
    if (this.encodedKey == null) {
      KeyGenerator generator = KeyGenerator.getInstance("AES");
      generator.init(256);
      SecretKey key = generator.generateKey();
      this.encodedKey = key.getEncoded(); // Get the key in encoded format
    }
    return this.encodedKey;
  }

  /**
   * Parses and validates a JWT token, if validation succeeded data is extracted.
   * 
   * @param jwt A JWT token string (either prefixed with bearer or not)
   * @return jwt claims
   */
  public Claims parse(String jwt) throws NoSuchAlgorithmException {
    if (jwt == null) {
      return null;
    }
    var claimsJws = jwt.toLowerCase().startsWith(AUTHENTICATION_SCHEME) ? jwt.substring(AUTHENTICATION_SCHEME.length()) : jwt;
    return Jwts.parser()
      .setSigningKey(getEncodedKey())
      .parseClaimsJws(claimsJws.trim()).getBody();
  }

  /**
   * Creates and sign a JSON web token with an encoded key.
   * TODO: include a refresh token
   *
   * @param subject Subject (a unique id; email)
   * @param issuer Issuer (this service)
   * @param data Data (user details) to be part of the claims
   * @return jwt
   */
  public <T extends Map<String, Object>> String create(String subject, String issuer, T data)
  throws NoSuchAlgorithmException {
    // A signed token only live as long as its expiration time and the rotation time
    var rotationTime = Long.parseLong(System.getProperty("JWT_ROTATION_TIME"));
    log.debug(String.format("Use rotation time in minutes: %s", rotationTime));

    var expirationTime = LocalDateTime.now().plusMinutes(rotationTime); // Rotate each 24 hours
    log.debug(String.format("Set expiration time to: %s", rotationTime));

    return Jwts.builder()
      .setClaims(Jwts.claims(data))
      .setSubject(subject) // A unique ID
      .setIssuer(issuer) // This service
      .setIssuedAt(new Date())
      .setExpiration(Date.from(expirationTime.atZone(ZoneId.systemDefault()).toInstant()))
      .signWith(SignatureAlgorithm.HS512, getEncodedKey())
      .compact();
  }
}
