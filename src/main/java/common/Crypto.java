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
  private static final String DIGEST_ALG = "SHA-256";
  private static final String KEY_SPEC_ALG = "AES";
  private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LEN = 12;
  private static final int GCM_TAG_LEN = 16;

  /** Private constructor */
  private Crypto() {
    throw new InstantiationError("Forbidden instantiation");
  }

  /**
   * Create a crypto key.
   * @param secret Key secret
   * @return crypto key
   */
  private static SecretKeySpec getSecretKey(String secret)
  throws GeneralSecurityException {
    var sha = MessageDigest.getInstance(DIGEST_ALG);
    var bytes = secret.getBytes(StandardCharsets.UTF_8);
    var digest = sha.digest(bytes);
    var key = Arrays.copyOf(digest, 16);
    return new SecretKeySpec(key, KEY_SPEC_ALG);
  }

  /**
   * Encrypt a string.
   * @param secret Key secret
   * @param str A string to be encrypted
   * @return encrypted string
   */
  public static String encrypt(String secret, String str)
  throws GeneralSecurityException {
    var iv = new byte[GCM_IV_LEN];

    var random = new SecureRandom();
    random.nextBytes(iv);

    var cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
    var spec = new GCMParameterSpec(GCM_TAG_LEN * Byte.SIZE, iv);
    var key = getSecretKey(secret);
    cipher.init(Cipher.ENCRYPT_MODE, key, spec);

    var cipherText = cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
    var encrypted = new byte[iv.length + cipherText.length];
    System.arraycopy(iv, 0, encrypted, 0, iv.length);
    System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);

    return Base64.getEncoder().encodeToString(encrypted);
  }

  /**
   * Decrypt an encrypted string.
   * @param secret Key secret
   * @param str Encrypted string
   * @return decrypted string
   */
  public static String decrypt(String secret, String str)
  throws GeneralSecurityException {
    var decoded = Base64.getDecoder().decode(str);

    var iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LEN);

    var cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
    var spec = new GCMParameterSpec(GCM_TAG_LEN * Byte.SIZE, iv);
    var key = getSecretKey(secret);
    cipher.init(Cipher.DECRYPT_MODE, key, spec);

    var cipherText = cipher.doFinal(decoded, GCM_IV_LEN, decoded.length - GCM_IV_LEN);

    return new String(cipherText, StandardCharsets.UTF_8);
  }
}
