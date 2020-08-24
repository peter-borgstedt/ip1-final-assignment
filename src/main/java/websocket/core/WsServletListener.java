package websocket.core;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units.
 *
 * Use a filter to retrieve some details about the request before
 * the websocket hand shake as these details are later not available
 * in the websocket context. Details collected are host remote host name,
 * remote adress and remote port, these are used for loggings.
 *
 * References:
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
@WebListener
public class WsServletListener implements ServletRequestListener {
  private static final Logger log = LogManager.getLogger(WsServletListener.class);

  @Override
  public void requestInitialized(ServletRequestEvent event) {
    var request = (HttpServletRequest)event.getServletRequest();

    // Set some extra attributes that will be used later and is only available during the request
    request.getSession().setAttribute("remoteHost", request.getRemoteHost());
    request.getSession().setAttribute("remoteAddr", request.getRemoteAddr());
    request.getSession().setAttribute("remotePort", request.getRemotePort());

    var serverPort = request.getServerPort();
    if ((serverPort == 80) || (serverPort == 443)) {
      // No need to add the server port for standard HTTP and HTTPS ports, the scheme will help determine it.
      var url = String.format("%s://%s", request.getScheme(), request.getServerName());
      log.info(String.format("Server host: %s", url));
      request.getSession().setAttribute("serverHost", url);
    } else {
      var url = String.format("%s://%s:%s", request.getScheme(), request.getServerName(), serverPort);
      log.info(String.format("Server host: %s", url));
      request.getSession().setAttribute("serverHost", url);
    }
  }
}