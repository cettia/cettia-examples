import io.cettia.example.platform.play2.{Bootstrap => T}

import play.api.GlobalSettings
import play.api.mvc._
import play.core.j._

object Global extends GlobalSettings {
  override def onRouteRequest(req: RequestHeader): Option[Handler] = {
    if (req.path == "/cettia") {
      if (req.method == "GET" && req.headers.get("Upgrade").exists(_.equalsIgnoreCase("websocket"))) {
        Some(JavaWebSocket.ofString(T.websocket))
      } else {
        Some(new JavaAction {
          val annotations = new JavaActionAnnotations(classOf[T], classOf[T].getMethod("http"))
          val parser = annotations.parser
          def invocation = T.http
        })
      }
    } else {
      super.onRouteRequest(req)
    }
  }
}