package common;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Symmetric encryption and decryption using AES with GCP (Galois/Counter Mode).
 * This is according to best practice and the encryption mode recommended by SonarLint;
 * https://rules.sonarsource.com/java/RSPEC-5542
 *
 * References:
 * https://stackoverflow.com/questions/992019/java-256-bit-aes-password-based-encryption
 * https://datalocker.com/what-is-the-difference-between-ecb-mode-versus-cbc-mode-aes-encryption/
 * https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
 * https://stackoverflow.com/a/46155266
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Crypto {
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 16;
  private static SecureRandom secureRandom = new SecureRandom();

  private Crypto() {
    throw new InstantiationError("Forbidden instantiation");
  }

  private static SecretKeySpec getSecretKey(String secret)
  throws GeneralSecurityException {
    MessageDigest sha = MessageDigest.getInstance("SHA-256");
    var bytes = secret.getBytes(StandardCharsets.UTF_8);
    var digest = sha.digest(bytes);
    var key = Arrays.copyOf(digest, 16);
    return new SecretKeySpec(key, "AES");
  }


  public static String encrypt(String secret, String str)
  throws GeneralSecurityException {
    byte[] iv = new byte[GCM_IV_LENGTH];
    (new SecureRandom()).nextBytes(iv);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
    var key = getSecretKey(secret);
    cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

    byte[] cipherText = cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
    byte[] encrypted = new byte[iv.length + cipherText.length];
    System.arraycopy(iv, 0, encrypted, 0, iv.length);
    System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);

    return Base64.getEncoder().encodeToString(encrypted);
  }

  public static String decrypt(String secret, String str)
  throws GeneralSecurityException {
    byte[] decoded = Base64.getDecoder().decode(str);

    byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
    var key = getSecretKey(secret);
    cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

    byte[] cipherText = cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH);

    return new String(cipherText, StandardCharsets.UTF_8);
  }
}
