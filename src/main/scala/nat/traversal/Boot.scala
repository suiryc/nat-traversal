package nat.traversal

import grizzled.slf4j.Logging
import nat.traversal.upnp.UPnPManager
import nat.traversal.upnp.ssdp.SSDPClientService


object Boot
  extends App
  with Logging
{

  /* It's useful to get a logger here, so that logging system gets initialized.
   * Otherwise we are likely to get messages about loggers not working because
   * instantiated during initialization phase.
   * See: http://www.slf4j.org/codes.html#substituteLogger
   */

  UPnPManager.startServer()

  SSDPClientService.discover(UPnPManager.system)

  //Thread.sleep(10000)
  //UPnPManager.stopServer()
}
