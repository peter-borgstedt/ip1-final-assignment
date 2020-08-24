package datastructures;

import java.io.Serializable;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Class contains data for a text message.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class Text implements Serializable {
  private static final long serialVersionUID = 3L;

  public String text;

  /** Empty constructor; for populating public fields from outside */
  public Text() {}

  /**
   * Constructor.
   * @param text Text message
   */
  public Text(String text) {
    this.text = text;
  }
};
