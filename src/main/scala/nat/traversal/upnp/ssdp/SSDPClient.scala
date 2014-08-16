package nat.traversal.upnp.ssdp

import java.net.{
  InetAddress,
  Inet4Address,
  InetSocketAddress,
  NetworkInterface
}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import grizzled.slf4j.Logging
import java.net.URL
import nat.traversal.util.HTTPParser
import nat.traversal.util.RFC2616
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.xml.NodeSeq
import spray.client.pipelining._
import spray.http.HttpRequest
import nat.traversal.upnp.igd.{GatewayDevice, GatewayDeviceDesc, Protocol}


object SSDPClientService extends Logging {

  private case class DeviceDescription(node: NodeSeq, base: URL, localAddress: InetSocketAddress)

  def discover(implicit system: ActorSystem) {
    for (address <- getInetAddresses) {
      logger.debug(s"SSDP discover on $address")

      /* Note: we need one actor per interface, because it is bound to the
       * concerned socket. */
      implicit val ssdpClientService = system.actorOf(Props[SSDPClientService],
        s"upnp-service-ssdp-client-${address.getHostAddress}")

      IO(Udp) ! Udp.Bind(ssdpClientService, new InetSocketAddress(address, 0))
    }
  }

  private def getNetworkInterfaces: List[NetworkInterface] =
    try {
      import scala.collection.JavaConversions._

      /* getNetworkInterfaces can return null */
      Option(NetworkInterface.getNetworkInterfaces).map(_.toList).
        getOrElse(Nil).filter
      { interface =>
        logger.debug(s"Found interface $interface")
        /* Interface needs to:
         *  - be up
         *  - not be virtual
         *  - not be loopback
         *  - not be point to point: we only want to use local network
         *  - support multicast: since we are about to use it
         */
        interface.isUp && !interface.isVirtual && !interface.isLoopback &&
        !interface.isPointToPoint && interface.supportsMulticast()
      }
    }
    catch {
      case _: Throwable =>
        /* XXX - log */
        Nil
    }

  private def getInetAddresses(interface: NetworkInterface): List[InetAddress] =
  {
    import scala.collection.JavaConversions._

    interface.getInetAddresses.toList.filter { address =>
      /* Drop IPv6, we don't need/handle it */
      address.isInstanceOf[Inet4Address]
    }
  }

  private def getInetAddresses: List[InetAddress] =
    for {
      interface <- getNetworkInterfaces
      address <- getInetAddresses(interface)
    } yield address

}


class SSDPClientService
  extends Actor
  with Logging
{

  import SSDPClientService._

  /* http://en.wikipedia.org/wiki/Internet_Gateway_Device_Protocol
   * http://en.wikipedia.org/wiki/Simple_Service_Discovery_Protocol
   * http://en.wikipedia.org/wiki/HTTPU
   *
   * We are using HTTPU, so better end with an empty line after headers.
   */
  private val ssdpMsg =
    "M-SEARCH * HTTP/1.1\r\n" +
    "Host: 239.255.255.250:1900\r\n" +
    "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n" +
    "Man: \"ssdp:discover\"\r\n" +
    "MX: 1\r\n" +
    "\r\n"

  def receive = {
    case Udp.Bound(local) =>
      /* Send the unicast request to discover gateways */
      sender ! Udp.Send(
          ByteString(ssdpMsg, "US-ASCII"),
          new InetSocketAddress("239.255.255.250", 1900)
        )
      context.become(ready(sender(), local))
  }

  def ready(socket: ActorRef, localAddress: InetSocketAddress): Receive = {
    case Udp.Received(data, remote) =>
      HTTPParser(data.utf8String) match {
        case Left(failure) =>
          logger.error(s"SSDP client received invalid message from ${remote.getAddress}: message[${data.utf8String}] error[$failure]")

        case Right(httpMsg) =>
          logger.trace(s"SSDP client received message from ${remote.getAddress}: ${httpMsg.headers}")
          httpMsg match {
            case request: RFC2616.Request =>
            case response: RFC2616.Response =>
              if ((response.statusCode == 200) &&
                (response.headers.getOrElse("St", "") == "urn:schemas-upnp-org:device:InternetGatewayDevice:1"))
              {
                response.headers.get("Location").foreach { location =>
                  logger.debug(s"Found SSDP server location: $location")
                  /* execution context for futures */
                  import context.dispatcher
                  val pipeline: HttpRequest => Future[NodeSeq] = (
                    sendReceive
                    ~> unmarshal[NodeSeq]
                  )
                  pipeline(Get(location)).onComplete {
                    case Success(x) =>
                      val locationUrl = new URL(location)
                      val base = new URL(locationUrl.getProtocol,
                        locationUrl.getHost, locationUrl.getPort, "")
                      self ! DeviceDescription(x, base, localAddress)

                    case Failure(x) =>
                      /* XXX */
                  }
                }
              }
          }
      }

    case Udp.Unbind =>
      socket ! Udp.Unbind

    case Udp.Unbound =>
      context.stop(self)

    case DeviceDescription(node, base, localAddress) =>
      logger.trace(s"SSDP client got XML response: [$node]")
      try {
        import context.dispatcher
        val deviceDesc = GatewayDeviceDesc(node, base)
        val builder = new StringBuilder()
        logger.debug(s"Found gateway device: $deviceDesc\n${deviceDesc.prettyString(builder, 0)}")
        val igd = new GatewayDevice(deviceDesc, localAddress)
        for (wanDevice <- igd.wanDevices) {
          for (linkProperties <- wanDevice.linkProperties) {
            println(linkProperties.accessType)
            println(linkProperties.downstreamMaxBitRate)
            println(linkProperties.upstreamMaxBitRate)
          }
          for (wanConnection <- wanDevice.wanConnections) {
            println(wanConnection.status)
            println(wanConnection.externalIPAddress)
            for (portMapping <- wanConnection.portMappings)
              println(portMapping)
            wanConnection.addPortMapping(6666)
            for (portMapping <- wanConnection.portMappings)
              println(portMapping)
            wanConnection.deletePortMapping(6666)
            for (portMapping <- wanConnection.portMappings)
              println(portMapping)
          }
        }
      }
      catch {
        case e: Throwable =>
          logger.error(s"Found invalid gateway device", e)
      }

    case rest =>
      logger.trace(s"SSDP client got rest[$rest]")
  }

}
