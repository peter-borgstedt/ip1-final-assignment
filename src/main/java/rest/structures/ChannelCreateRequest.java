package rest.structures;

import javax.validation.constraints.NotBlank;
/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * Request body for creating a channel.
 * 
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class ChannelCreateRequest {
  @NotBlank(message = "Property cannot be null or empty")
  public String name;
  public String description;
}