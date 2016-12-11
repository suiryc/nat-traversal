package nat.traversal.upnp.igd

import akka.actor.ActorSystem
import akka.stream.Materializer
import java.net.{InetSocketAddress, URL}
import nat.traversal.util.NodeOps
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq


/**
 * Internet Gateway Device.
 */
class GatewayDevice(
  val desc: GatewayDeviceDesc,
  val localAddress: InetSocketAddress
)(implicit system: ActorSystem, executionContext: ExecutionContext, materializer: Materializer)
{

  val device = new Device(desc.device, localAddress)

  /* An IntergetGatewayDevice may have more than one WANDevice. */
  val wanDevices: List[WANDevice] =
    device.getDevices("WANDevice").flatMap { wanDevice =>
      try {
        Some(new WANDevice(wanDevice.desc, localAddress))
      }
      catch {
        case _: Throwable =>
          None
      }
    }

}

/**
 * Internet Gateway Device description.
 *
 * As found in the root device description URL.
 */
class GatewayDeviceDesc(
  val specVersion: SpecVersion,
  val URLBase: Option[URL],
  val device: DeviceDesc
) extends Entity
{

  override def toString: String =
    "specVersion[" + specVersion +
    "] device[" + device +
    "]"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "specVersion", specVersion)
    prettyString(builder, level, "URLBase", URLBase)
    prettyString(builder, level, "device", device)
  }

}

object GatewayDeviceDesc extends NodeOps {

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @param base root of relative URLs
   * @return corresponding instance
   */
  def apply(node: NodeSeq, base: URL)
    (implicit system: ActorSystem, executionContext: ExecutionContext, materializer: Materializer)
    : GatewayDeviceDesc =
  {
    val specVersion: SpecVersion = SpecVersion(
      getChild(node, "specVersion")
    )
    val URLBase: Option[URL] = getChildOption(node, "URLBase").
      map(node => new URL(node.text))
    val device: DeviceDesc = DeviceDesc(
      getChild(node, "device"),
      URLBase.getOrElse(base)
    )

    new GatewayDeviceDesc(
      specVersion = specVersion,
      URLBase = URLBase,
      device = device
    )
  }

}
