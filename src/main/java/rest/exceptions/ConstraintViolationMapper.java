package rest.exceptions;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolationException;
import javax.validation.Path.Node;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.hibernate.validator.constraints.Length;
/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * A custom exception mapper; presents thrown exceptions in the response.
 * This includes all constraints set on the Request body for a REST resource
 * (see {@link org.hibernate.validator.constraints }).
 * 
 * This basically prints out errors in the response in a way I want as the standard
 * reveals alot and is not formatted that nicely.
 *
 * References:
 * https://stackoverflow.com/a/41856227
 * https://stackoverflow.com/questions/3293599/jax-rs-using-exception-mappers
 * https://howtodoinjava.com/resteasy/resteasy-exceptionmapper-example/
 * 
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {
  @Override
  public Response toResponse(final ConstraintViolationException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
                    .entity(prepareMessage(exception))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
  }

  @SuppressWarnings("unused")
  private Object prepareMessage(ConstraintViolationException exception) {
    var constraints = exception.getConstraintViolations();

    var response = new Object() {
      public String type = "VALIDATION";
      public List<Object> errors = new ArrayList<>();
    };

    for (var cv : constraints) {
      List<Node> list = new ArrayList<>();
      cv.getPropertyPath().iterator().forEachRemaining(list::add);
      var propertyName = list.get(list.size() - 1).getName();

      var annotation = cv.getConstraintDescriptor().getAnnotation();
      var annotationType = annotation.annotationType();

      if (annotationType == Length.class) {
        var lengthAnnotation = Length.class.cast(annotation);
        response.errors.add(new Object() {
          public String type = annotationType.getSimpleName().toUpperCase();
          public String property = propertyName;
          public String message = lengthAnnotation.message();
          public int min = lengthAnnotation.min();
          public int max = lengthAnnotation.max();
        });
      } else {
        response.errors.add(new Object() {
          public String type = annotationType.getSimpleName().toUpperCase();
          public String property = propertyName;
          public String message = cv.getMessage();
        });
      }
    }
    return response;
  }
}