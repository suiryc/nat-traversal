package nat.traversal.upnp.igd

import akka.actor.ActorRefFactory
import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext

/**
 * WAN device.
 */
class WANDevice(
  override val desc: DeviceDesc,
  override val localAddress: InetSocketAddress
)(implicit refFactory: ActorRefFactory, executionContext: ExecutionContext)
  extends Device(desc, localAddress)
{

  /* A WANDevice may have more than one WANConnectionDevice.
   * A WANConnectionDevice may have more than one WANIPConnection or
   * WANPPPConnection.
   */

  val wanConnections =
    (getDevices("WANConnectionDevice").map { cnxDevice =>
      (for {
        cnxService <- cnxDevice.getServices("WANIPConnection") :::
          cnxDevice.getServices("WANPPPConnection")
      } yield {
        try {
          Some(new WANConnection(this, cnxService))
        }
        catch {
          case e: Throwable =>
            None
        }
      }) flatten
    }) flatten

}
