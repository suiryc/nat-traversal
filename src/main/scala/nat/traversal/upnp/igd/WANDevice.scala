package nat.traversal.upnp.igd

import akka.actor.ActorSystem
import akka.stream.Materializer
import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext

/**
 * WAN device.
 */
class WANDevice(
  override val desc: DeviceDesc,
  override val localAddress: InetSocketAddress
)(implicit system: ActorSystem, executionContext: ExecutionContext, materializer: Materializer)
  extends Device(desc, localAddress)
{

  /* A WANDevice may have more than one WANConnectionDevice.
   * A WANConnectionDevice may have more than one WANIPConnection or
   * WANPPPConnection.
   */

  val wanConnections: List[WANConnection] =
    getDevices("WANConnectionDevice").flatMap { cnxDevice =>
      (for {
        cnxService <- cnxDevice.getServices("WANIPConnection") :::
          cnxDevice.getServices("WANPPPConnection")
      } yield {
        try {
          Some(new WANConnection(this, cnxService))
        }
        catch {
          case _: Throwable =>
            None
        }
      }).flatten
    }

}
