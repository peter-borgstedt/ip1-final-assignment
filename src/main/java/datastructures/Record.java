package datastructures;

import java.math.BigDecimal;
import java.util.Map;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains arbitrary data for a database record in some table.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Record {
  private Map<String, Object> data;

  /**
   * Constructor.
   * @param data Map containing data for a database record
   */
  public Record(Map<String, Object> data) {
    this.data = data;
  }

  /**
   * Get generic value to which the specified key is mapped to or
   * or null if not existing.
   * @param key key whose associated with the value
   * @return mapped value for key or null if not existing
   */
  public <T> T get(String key, Class<T> clazz) {
    return clazz.cast(data.get(key));
  }

  /**
   * Get String value to which the specified key is mapped to or
   * or null if not existing.
   * @param key key whose associated with the value
   * @return mapped value for key or null if not existing
   */
  public String getString(String key) {
    return get(key, String.class);
  }

  /**
   * Get Integer value to which the specified key is mapped to or
   * or null if not existing.
   * @param key key whose associated with the value
   * @return mapped value for key or null if not existing
   */
  public Integer getInteger(String key) {
    return get(key, Integer.class);
  }

  /**
   * Get Long value to which the specified key is mapped to or
   * or null if not existing.
   * @param key key whose associated with the value
   * @return mapped value for key or null if not existing
   */
  public Long getLong(String key) {
    return get(key, Long.class);
  }

  /**
   * Get BigDecimal value to which the specified key is mapped to or
   * or null if not existing.
   * @param key key whose associated with the value
   * @return mapped value for key or null if not existing
   */
  public BigDecimal getBigDecimal(String key) {
    return get(key, BigDecimal.class);
  }

  /**
   * Get byte[] value to which the specified key is mapped to or
   * or null if not existing.
   * @param key key whose associated with the value
   * @return mapped value for key or null if not existing
   */
  public byte[] getByteArray(String key) {
    return (byte[])data.get(key);
  }
}
