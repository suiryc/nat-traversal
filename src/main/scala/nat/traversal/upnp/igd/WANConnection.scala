package nat.traversal.upnp.igd

import akka.actor.ActorSystem
import java.text.SimpleDateFormat
import java.util.Date
import nat.traversal.util.{NodeConverters, NodeOps}
import scala.concurrent.ExecutionContext
import scala.xml.Node

/**
 * WAN connection.
 */
class WANConnection(
  val device: WANDevice,
  val service: Service
)(implicit system: ActorSystem, executionContext: ExecutionContext)
{

  /** Gets connection status. */
  def status: _root_.nat.traversal.upnp.igd.ConnectionStatus.Value = ConnectionStatus.withName(
    (service.action("GetStatusInfo")() \ "NewConnectionStatus").head.text.
    toLowerCase.capitalize
  )

  /** Gets connection external IP address. */
  def externalIPAddress: String =
    (service.action("GetExternalIPAddress")() \ "NewExternalIPAddress").head.text

  /** Gets connection port mappings. */
  def portMappings: List[PortMapping] = {
    def loop(idx: Int, mappings: List[PortMapping]): List[PortMapping] = try {
      val result = service.action("GetGenericPortMappingEntry")(
        Map("NewPortMappingIndex" -> idx)
      )

      loop(idx + 1, PortMapping(result) :: mappings)
    }
    catch {
      case _: Throwable =>
        mappings
    }

    loop(0, Nil).reverse
  }

  /**
   * Adds port mapping.
   *
   * @param externalPort external port, 0 to map all available ports
   * @param internalPort internal port, same as external if not given,
   *   unused when mapping all ports
   * @param protocol protocol to map, all if not given
   * @param description mapping description
   */
  def addPortMapping(
    externalPort: Int,
    internalPort: Option[Int] = None,
    internalClient: Option[String] = None,
    protocol: Option[Protocol.Value] = None,
    description: Option[String] = None
  ) {
    val _internalPort = internalPort.getOrElse(
      if (externalPort > 0) externalPort else 1
    )
    val _internalClient = internalClient.getOrElse(
      device.localAddress.getAddress.getHostAddress
    )
    val _protocols = protocol.map(_ :: Nil).getOrElse(
      Protocol.TCP :: Protocol.UDP :: Nil
    )

    for (protocol <- _protocols) {
      val _description = description.getOrElse(
        s"nat-traversal $protocol @ ${
          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        }"
      )

      service.action("AddPortMapping")(Map(
        "NewRemoteHost" -> "",
        "NewExternalPort" -> externalPort,
        "NewProtocol" -> protocol,
        "NewInternalPort" -> _internalPort,
        "NewInternalClient" -> _internalClient,
        "NewEnabled" -> "1",
        "NewPortMappingDescription" -> _description,
        "NewLeaseDuration" -> "0"
      ))
    }
  }

  /** Gets a specific port mapping if it exists. */
  def specificPortMapping(externalPort: Int, protocol: Protocol.Value) =
    PortMapping(
      service.action("GetSpecificPortMappingEntry")(Map(
        "NewRemoteHost" -> "",
        "NewExternalPort" -> externalPort,
        "NewProtocol" -> protocol
      )), "", externalPort, protocol
    )

  /**
   * Deletes a port mapping.
   *
   * @param externalPort external port, 0 for all available ports
   * @param protocol protocol to map, all if not given
   */
  def deletePortMapping(
    externalPort: Int,
    protocol: Option[Protocol.Value] = None
  ) {
    val _protocols = protocol.map(_ :: Nil).getOrElse(
      Protocol.TCP :: Protocol.UDP :: Nil
    )

    for (protocol <- _protocols) {
      service.action("DeletePortMapping")(Map(
        "NewRemoteHost" -> "",
        "NewExternalPort" -> externalPort,
        "NewProtocol" -> protocol
      ))
    }
  }

}

object ConnectionStatus extends Enumeration {

  val Unconfigured = Value
  val Connecting = Value
  val Connected = Value
  val PendingDisconnect = Value
  val Disconnecting = Value
  val Disconnected = Value

}

object Protocol extends Enumeration {

  val TCP = Value
  val UDP = Value

}

/**
 * Port mapping info.
 */
case class PortMapping(
  remoteHost: String,
  externalPort: Int,
  protocol: Protocol.Value,
  internalPort: Int,
  internalClient: String,
  enabled: Boolean,
  description: String,
  leaseDuration: Int
)
{

  override def toString: String =
    s"${
      if (remoteHost == "") "*" else remoteHost
    }:${
      if (externalPort == 0) "*" else externalPort
    }/$protocol -> $internalClient:${
      if (externalPort == 0) "*" else internalPort
    } ${
      if (enabled) "on" else "off"
    }${
      if (leaseDuration > 0) " during " + leaseDuration + "s"
      else ""
    } ($description)"

}

object PortMapping extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @return corresponding instance
   */
  def apply(node: Node): PortMapping =
    PortMapping(
      remoteHost = getChildValue[String](node, "NewRemoteHost"),
      externalPort = getChildValue[Int](node, "NewExternalPort"),
      protocol = Protocol.withName(getChildValue[String](node, "NewProtocol")),
      internalPort = getChildValue[Int](node, "NewInternalPort"),
      internalClient = getChildValue[String](node, "NewInternalClient"),
      enabled = getChildValue[Boolean](node, "NewEnabled"),
      description = getChildValue[String](node, "NewPortMappingDescription"),
      leaseDuration = getChildValue[Int](node, "NewLeaseDuration")
    )

  /**
   * Creates an instance from the corresponding XML node, with given values.
   *
   * @param node entity XML node
   * @param remoteHost actual remote host value
   * @param externalPort actual external port value
   * @param protocol actual protocol value
   * @return corresponding instance
   */
  def apply(node: Node, remoteHost: String, externalPort: Int,
      protocol: Protocol.Value)
    : PortMapping =
    PortMapping(
      remoteHost = remoteHost,
      externalPort = externalPort,
      protocol = protocol,
      internalPort = getChildValue[Int](node, "NewInternalPort"),
      internalClient = getChildValue[String](node, "NewInternalClient"),
      enabled = getChildValue[Boolean](node, "NewEnabled"),
      description = getChildValue[String](node, "NewPortMappingDescription"),
      leaseDuration = getChildValue[Int](node, "NewLeaseDuration")
    )

}
