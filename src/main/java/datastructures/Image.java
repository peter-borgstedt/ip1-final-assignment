package datastructures;

import java.io.Serializable;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a text message with an attached image,
 * the text is not mandatory and may be empty.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Image implements Serializable {
  private static final long serialVersionUID = 4L;

  public String text;
  public String imageUrl;

  /** Empty constructor; for populating public fields from outside */
  public Image() {}

  /**
   * Constructor.
   * @param text Text message (optional, may be null or empty)
   * @param imageUrl Image URL
   */
  public Image(String text, String imageUrl) {
    this.text = text;
    this.imageUrl = imageUrl;
  }
};
