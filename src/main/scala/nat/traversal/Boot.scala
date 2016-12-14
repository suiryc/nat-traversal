package nat.traversal

import com.typesafe.scalalogging.StrictLogging
import nat.traversal.upnp.UPnPManager
import nat.traversal.upnp.ssdp.SSDPClientService


object Boot extends StrictLogging {

  /* It's useful to get a logger here, so that logging system gets initialized.
   * Otherwise we are likely to get messages about loggers not working because
   * instantiated during initialization phase.
   * See: http://www.slf4j.org/codes.html#substituteLogger
   */

  import UPnPManager.system
  import UPnPManager.materializer

  def main(args: Array[String]): Unit = {
    UPnPManager.startServer()

    SSDPClientService.discover

    //Thread.sleep(10000)
    //UPnPManager.stopServer()
  }

}
