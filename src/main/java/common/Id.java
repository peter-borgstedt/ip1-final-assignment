package common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Generates a simple unique ID depending on the current time in milliseconds (epoch),
 * maps each number character with a URL friendly replacement character which will also
 * result in a short representation (as the character maps has a larger base than 10).
 *
 * References:
 * https://stackoverflow.com/a/10981113
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Id {
  private static final char[] CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

  /** Private constructor */
  private Id() {
    throw new InstantiationError("Forbidden instantiation");
  }

  /**
   * Converts the of 10 to the base of 36 (CHARACTER.length).
   * @param number The number to be converted
   * @param base The base to be converted into
   */
  private static List<Integer> convertBase(Long number, Integer base) {
    var result = new ArrayList<Integer>();
    while (number > 0) {
      result.add((int)(number % base)); // Get position in character map
      number = number / base;
    }
    Collections.reverse(result);
    return result;
  }

  /**
   * Generate a ID based on the current time.
   * Uses the current time in milliseconds that is then converted from base 10 to base of 36,
   * which is the base of a character map that will the be used to map to a character representing
   * the new value/entity/position, these are then joined together to a string.
   * @return a shorted string representing a timestamp
   */
  public static String generate() {
    var token = convertBase(Instant.now().toEpochMilli(), CHARACTERS.length);
    return token.stream()
      .map(e -> CHARACTERS[e])
      .map(String::valueOf)
      .collect(Collectors.joining());
  }
}
