package datastructures;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains binary data an image and the what file format
 * it is of (jpg, gif, png etc).
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class ImageData {
  public final String format;
  public final byte[] data;

  /**
   * Constructor.
   * @param format File format (jpg, gif, png etc)
   * @param data Image data
   */
  public ImageData(String format, byte[] data) {
    this.format = format;
    this.data = data;
  }
};
