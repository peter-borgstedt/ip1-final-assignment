package common;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class with some static methods for conversion between JSON and Java objects..
 * Inspiration is taken from the JSON.stringify() and JSON.parse() in the JavaScript world.
 *
 * References:
 * https://www.journaldev.com/2324/jackson-json-java-parser-api-example-tutorial
 * https://mkyong.com/java/java-convert-object-to-map-example/
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Json {
  private Json() {
    throw new InstantiationError("Forbidden instantiation");
  }

  /**
   * JSON to Object, parse; transform; map JSON to Object.
   * into an object of specified class.
   *
   * @param <T> Object class type
   * @param data A byte array of a JSON (formatted string)
   * @param clazz Class of object to be transformed into
   * @return object of generic type if successfully parsed (mapped)
   */
  public static <T> T parse(byte[] data, Class<T> clazz) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(data, clazz);
  }

  /**
   * JSON to Object, parse; transform; map JSON to Object.
   * into an object of specified class.
   *
   * @param <T> Object class type
   * @param data A JSON (formatted string)
   * @param clazz Class of object to be transformed into
   * @return object of generic type if successfully parsed (mapped)
   */
  public static <T> T parse(String data, Class<T> clazz) throws IOException {
    return Json.parse(data.getBytes(StandardCharsets.UTF_8), clazz);
  }

  /**
   * Object to JSON, parse; transform; map Object to JSON.
   * into an object of specified class.
   * @param object Object to be transformed into a JSON (formatted string)
   * @return a JSON (formatted string) with properties from specified object
   */
  public static String stringify(Object object) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    StringWriter sw = new StringWriter();
    objectMapper.writeValue(sw, object);
    return sw.toString();
  }

  /**
   * Convert an object to a {@link java.util.Map} with its fields (properties).
   * @param obj Object to convert to a Map
   * @return a map with the objects fields (properties)
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> objectToMap(Object obj) {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(obj, Map.class);
 }
}
