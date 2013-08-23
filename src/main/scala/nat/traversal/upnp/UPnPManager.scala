package nat.traversal.upnp

import akka.actor.ActorSystem
import nat.traversal.upnp.ssdp.SSDPServerService

object UPnPManager {

  val system = ActorSystem("nat-traversal-upnp")

  def startServer() {
    SSDPServerService.start()
  }

  def stopServer() {
    SSDPServerService.stop()
  }

}
