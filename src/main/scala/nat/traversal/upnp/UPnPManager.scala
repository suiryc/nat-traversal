package nat.traversal.upnp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import nat.traversal.upnp.ssdp.SSDPServerService

object UPnPManager {

  implicit val system = ActorSystem("nat-traversal-upnp")

  implicit val materializer = ActorMaterializer()

  def startServer() {
    SSDPServerService.start()
  }

  def stopServer() {
    SSDPServerService.stop()
  }

}
