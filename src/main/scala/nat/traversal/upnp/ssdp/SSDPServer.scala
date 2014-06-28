package nat.traversal.upnp.ssdp

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import grizzled.slf4j.Logging
import java.net.{InetAddress, MulticastSocket}
import nat.traversal.upnp.UPnPManager
import nat.traversal.util.{HTTPParser, RFC2616, UdpMc, UdpMcService}
import scala.xml.NodeSeq
import spray.can.Http
import spray.http.MediaTypes
import spray.http.HttpHeaders.Connection
import spray.routing.HttpService


/* Our service behavior, defined independently from the service actor.
 * Allows separate testing without having to create a whole system.
 */
trait SSDPServerHttp extends HttpService {

  val myRoute =
    /* XXX - use device UUID as path prefix (would allow to have more than one mock device) */
    path("desc" / "root") {
      get {
        respondWithHeader(Connection("close")) {
          respondWithMediaType(MediaTypes.`text/xml`) {
            complete {
"<?xml version=\"1.0\"?>" +
<root xmlns="urn:schemas-upnp-org:device-1-0">
  <specVersion>
    <major>1</major>
    <minor>0</minor>
  </specVersion>
  <device>
    <deviceType>urn:schemas-upnp-org:device:InternetGatewayDevice:1</deviceType>
    <friendlyName>Dummy InternetGatewayDevice</friendlyName>
    <manufacturer>Dummy manufacturer</manufacturer>
    <manufacturerURL>http://manufacturer.dummy.org/</manufacturerURL>
    <modelDescription>Dummy device description</modelDescription>
    <modelName>Dummy device name</modelName>
    <modelNumber>Dummy device model number</modelNumber>
    <modelURL>http://device.manufacturer.dummy.org/</modelURL>
    <serialNumber>Dummy device serial number</serialNumber>
    <UDN>uuid:igd01234567-0123-0123-0123-0123456789ab</UDN>
    <UPC>Dummy Universal Product Code</UPC>
    <serviceList>
    </serviceList>
    <deviceList>
      <device>
        <deviceType>urn:schemas-upnp-org:device:WANDevice:1</deviceType>
        <friendlyName>Dummy WANDevice</friendlyName>
        <manufacturer>Dummy manufacturer</manufacturer>
        <manufacturerURL>http://manufacturer.dummy.org/</manufacturerURL>
        <modelDescription>Dummy device description</modelDescription>
        <modelName>Dummy device name</modelName>
        <modelNumber>Dummy device model number</modelNumber>
        <modelURL>http://device.manufacturer.dummy.org/</modelURL>
        <serialNumber>Dummy device serial number</serialNumber>
        <UDN>uuid:wan01234567-0123-0123-0123-0123456789ab</UDN>
        <serviceList>
        </serviceList>
        <deviceList>
          <device>
            <deviceType>urn:schemas-upnp-org:device:WANConnectionDevice:1</deviceType>
            <friendlyName>Dummy WANConnectionDevice</friendlyName>
            <manufacturer>Dummy manufacturer</manufacturer>
            <manufacturerURL>http://manufacturer.dummy.org/</manufacturerURL>
            <modelDescription>Dummy device description</modelDescription>
            <modelName>Dummy device name</modelName>
            <modelNumber>Dummy device model number</modelNumber>
            <modelURL>http://device.manufacturer.dummy.org/</modelURL>
            <serialNumber>Dummy device serial number</serialNumber>
            <UDN>uuid:wanc01234567-0123-0123-0123-0123456789ab</UDN>
            <serviceList>
              <service>
                <serviceType>urn:schemas-upnp-org:service:WANIPConnection:1</serviceType>
                <serviceId>urn:upnp-org:serviceId:WANIPConn1</serviceId>
                <SCPDURL>/desc/wan_ip_connection</SCPDURL>
                <controlURL>/control/wan_ip_connection</controlURL>
                <eventSubURL>/event/wan_ip_connection</eventSubURL>
              </service>
            </serviceList>
          </device>
        </deviceList>
      </device>
    </deviceList>
    <presentationURL>http://mydevice.manufacturer.dummy.org/</presentationURL>
  </device>
</root>
            }
          }
        }
      }
    } ~
    path("desc" / "wan_ip_connection") {
      get {
        respondWithHeader(Connection("close")) {
          respondWithMediaType(MediaTypes.`text/xml`) {
            complete {
"<?xml version=\"1.0\"?>" +
<scpd xmlns="urn:schemas-upnp-org:service-1-0">
  <specVersion>
    <major>1</major>
    <minor>0</minor>
  </specVersion>
  <actionList>
    <action>
      <name>GetStatusInfo</name>
      <argumentList>
        <argument>
          <name>NewConnectionStatus</name>
          <direction>out</direction>
          <relatedStateVariable>ConnectionStatus</relatedStateVariable>
        </argument>
        <argument>
          <name>NewLastConnectionError</name>
          <direction>out</direction>
          <relatedStateVariable>LastConnectionError</relatedStateVariable>
        </argument>
        <argument>
          <name>NewUptime</name>
          <direction>out</direction>
          <relatedStateVariable>Uptime</relatedStateVariable>
        </argument>
      </argumentList>
    </action>
    <action>
      <name>GetGenericPortMappingEntry</name>
      <argumentList>
        <argument>
          <name>NewPortMappingIndex</name>
          <direction>in</direction>
          <relatedStateVariable>PortMappingNumberOfEntries</relatedStateVariable>
        </argument>
        <argument>
          <name>NewRemoteHost</name>
          <direction>out</direction>
          <relatedStateVariable>RemoteHost</relatedStateVariable>
        </argument>
        <argument>
          <name>NewExternalPort</name>
          <direction>out</direction>
          <relatedStateVariable>ExternalPort</relatedStateVariable>
        </argument>
        <argument>
          <name>NewProtocol</name>
          <direction>out</direction>
          <relatedStateVariable>PortMappingProtocol</relatedStateVariable>
        </argument>
        <argument>
          <name>NewInternalPort</name>
          <direction>out</direction>
          <relatedStateVariable>InternalPort</relatedStateVariable>
        </argument>
        <argument>
          <name>NewInternalClient</name>
          <direction>out</direction>
          <relatedStateVariable>InternalClient</relatedStateVariable>
        </argument>
        <argument>
          <name>NewEnabled</name>
          <direction>out</direction>
          <relatedStateVariable>PortMappingEnabled</relatedStateVariable>
        </argument>
        <argument>
          <name>NewPortMappingDescription</name>
          <direction>out</direction>
          <relatedStateVariable>PortMappingDescription</relatedStateVariable>
        </argument>
        <argument>
          <name>NewLeaseDuration</name>
          <direction>out</direction>
          <relatedStateVariable>PortMappingLeaseDuration</relatedStateVariable>
        </argument>
      </argumentList>
    </action>
    <action>
      <name>GetSpecificPortMappingEntry</name>
      <argumentList>
        <argument>
          <name>NewRemoteHost</name>
          <direction>in</direction>
          <relatedStateVariable>RemoteHost</relatedStateVariable>
        </argument>
        <argument>
          <name>NewExternalPort</name>
          <direction>in</direction>
          <relatedStateVariable>ExternalPort</relatedStateVariable>
        </argument>
        <argument>
          <name>NewProtocol</name>
          <direction>in</direction>
          <relatedStateVariable>PortMappingProtocol</relatedStateVariable>
        </argument>
        <argument>
          <name>NewInternalPort</name>
          <direction>out</direction>
          <relatedStateVariable>InternalPort</relatedStateVariable>
        </argument>
        <argument>
          <name>NewInternalClient</name>
          <direction>out</direction>
          <relatedStateVariable>InternalClient</relatedStateVariable>
        </argument>
        <argument>
          <name>NewEnabled</name>
          <direction>out</direction>
          <relatedStateVariable>PortMappingEnabled</relatedStateVariable>
        </argument>
        <argument>
          <name>NewPortMappingDescription</name>
          <direction>out</direction>
          <relatedStateVariable>PortMappingDescription</relatedStateVariable>
        </argument>
        <argument>
          <name>NewLeaseDuration</name>
          <direction>out</direction>
          <relatedStateVariable>PortMappingLeaseDuration</relatedStateVariable>
        </argument>
      </argumentList>
    </action>
    <action>
      <name>AddPortMapping</name>
      <argumentList>
        <argument>
          <name>NewRemoteHost</name>
          <direction>in</direction>
          <relatedStateVariable>RemoteHost</relatedStateVariable>
        </argument>
        <argument>
          <name>NewExternalPort</name>
          <direction>in</direction>
          <relatedStateVariable>ExternalPort</relatedStateVariable>
        </argument>
        <argument>
          <name>NewProtocol</name>
          <direction>in</direction>
          <relatedStateVariable>PortMappingProtocol</relatedStateVariable>
        </argument>
        <argument>
          <name>NewInternalPort</name>
          <direction>in</direction>
          <relatedStateVariable>InternalPort</relatedStateVariable>
        </argument>
        <argument>
          <name>NewInternalClient</name>
          <direction>in</direction>
          <relatedStateVariable>InternalClient</relatedStateVariable>
        </argument>
        <argument>
          <name>NewEnabled</name>
          <direction>in</direction>
          <relatedStateVariable>PortMappingEnabled</relatedStateVariable>
        </argument>
        <argument>
          <name>NewPortMappingDescription</name>
          <direction>in</direction>
          <relatedStateVariable>PortMappingDescription</relatedStateVariable>
        </argument>
        <argument>
          <name>NewLeaseDuration</name>
          <direction>in</direction>
          <relatedStateVariable>PortMappingLeaseDuration</relatedStateVariable>
        </argument>
      </argumentList>
    </action>
    <action>
      <name>DeletePortMapping</name>
      <argumentList>
        <argument>
          <name>NewRemoteHost</name>
          <direction>in</direction>
          <relatedStateVariable>RemoteHost</relatedStateVariable>
        </argument>
        <argument>
          <name>NewExternalPort</name>
          <direction>in</direction>
          <relatedStateVariable>ExternalPort</relatedStateVariable>
        </argument>
        <argument>
          <name>NewProtocol</name>
          <direction>in</direction>
          <relatedStateVariable>PortMappingProtocol</relatedStateVariable>
        </argument>
      </argumentList>
    </action>
    <action>
      <name>GetExternalIPAddress</name>
      <argumentList>
        <argument>
          <name>NewExternalIPAddress</name>
          <direction>out</direction>
          <relatedStateVariable>ExternalIPAddress</relatedStateVariable>
        </argument>
      </argumentList>
    </action>
  </actionList>
  <serviceStateTable>
    <stateVariable sendEvents="yes">
      <name>ConnectionStatus</name>
      <dataType>string</dataType>
      <allowedValueList>
        <allowedValue>Unconfigured</allowedValue>
        <allowedValue>Connecting</allowedValue>
        <allowedValue>Connected</allowedValue>
        <allowedValue>PendingDisconnect</allowedValue>
        <allowedValue>Disconnecting</allowedValue>
        <allowedValue>Disconnected</allowedValue>
      </allowedValueList>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>Uptime</name>
      <dataType>ui4</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>LastConnectionError</name>
      <dataType>string</dataType>
      <allowedValueList>
        <allowedValue>ERROR_NONE</allowedValue>
        <allowedValue>ERROR_COMMAND_ABORTED</allowedValue>
        <allowedValue>ERROR_NOT_ENABLED_FOR_INTERNET</allowedValue>
        <allowedValue>ERROR_USER_DISCONNECT</allowedValue>
        <allowedValue>ERROR_ISP_DISCONNECT</allowedValue>
        <allowedValue>ERROR_IDLE_DISCONNECT</allowedValue>
        <allowedValue>ERROR_FORCED_DISCONNECT</allowedValue>
        <allowedValue>ERROR_NO_CARRIER</allowedValue>
        <allowedValue>ERROR_IP_CONFIGURATION</allowedValue>
        <allowedValue>ERROR_UNKNOWN</allowedValue>
      </allowedValueList>
    </stateVariable>
    <stateVariable sendEvents="yes">
      <name>ExternalIPAddress</name>
      <dataType>string</dataType>
    </stateVariable>
    <stateVariable sendEvents="yes">
      <name>PortMappingNumberOfEntries</name>
      <dataType>ui2</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>PortMappingEnabled</name>
      <dataType>boolean</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>PortMappingLeaseDuration</name>
      <dataType>ui4</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>RemoteHost</name>
      <dataType>string</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>ExternalPort</name>
      <dataType>ui2</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>InternalPort</name>
      <dataType>ui2</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>PortMappingProtocol</name>
      <dataType>string</dataType>
      <allowedValueList>
        <allowedValue>TCP</allowedValue>
        <allowedValue>UDP</allowedValue>
      </allowedValueList>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>InternalClient</name>
      <dataType>string</dataType>
    </stateVariable>
    <stateVariable sendEvents="no">
      <name>PortMappingDescription</name>
      <dataType>string</dataType>
    </stateVariable>
  </serviceStateTable>
</scpd>
            }
          }
        }
      }
    } ~
    path("control" / "wan_ip_connection") {
      post {
        entity(as[NodeSeq]) { node =>
          complete {
            <a/>
          }
        }
      }
    }

}

class SSDPServerService 
  extends Actor
  with SSDPServerHttp
  with Logging
{

  /* the HttpService trait defines only one abstract member, which
   * connects the services environment to the enclosing actor or test */
  def actorRefFactory = context

  def httpReceive = runRoute(myRoute)

  def receive = httpReceive orElse {
    case UdpMc.Bound =>
      context.become(ready(sender) orElse httpReceive)
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      /* XXX - use interface IP, and HTTP server port */
      HTTPParser(data.utf8String) match {
        case Left(failure) =>
          logger.error(s"SSDP server received invalid message from ${remote.getAddress()}: message[${data.utf8String}] error[${failure}]")

        case Right(httpMsg) =>
          logger.debug(s"SSDP server received message from ${remote.getAddress()}: ${httpMsg.headers}")
          httpMsg match {
            case request: RFC2616.Request =>
              if ((request.method == "M-SEARCH") &&
                (request.requestURI == "*") &&
                (request.headers.getOrElse("St", "") == "urn:schemas-upnp-org:device:InternetGatewayDevice:1")
              ) {
                val msg =
                  "HTTP/1.1 200 OK\r\n" +
                  "SERVER: DummyOS/1.0 UPnP/1.0 dummyigdd/1.0\r\n" +
                  "LOCATION: http://192.168.0.16:5678/desc/root\r\n" +
                  "EXT:\r\n" +
                  "CACHE-CONTROL: max-age=1800\r\n" +
                  "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n" +
                  "USN: uuid:igd73616d61-6a65-7374-650a-0007cbce1dc0::urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n" +
                  "\r\n"
                socket ! Udp.Send(ByteString(msg, "US-ASCII"), remote)
              }

            case _ =>
              /* XXX */
          }
      }

    case Udp.Unbind =>
      socket ! Udp.Unbind

    case Udp.Unbound =>
      context.stop(self)
  }

}

object SSDPServerService {

  implicit val system = UPnPManager.system

  val ssdpServerService = system.actorOf(Props[SSDPServerService], "upnp-service-ssdp-server")
  val udpService = system.actorOf(Props[UdpMcService], "upnp-service-udp")

  def start() {
    IO(Http) ! Http.Bind(ssdpServerService, interface = "0.0.0.0", port = 5678)

    val socket = new MulticastSocket(1900)
    socket.joinGroup(InetAddress.getByName("239.255.255.250"))

    implicit val sender = ssdpServerService
    udpService ! UdpMc.Bind(ssdpServerService, socket)
  }

  def stop() {
    ssdpServerService ! Udp.Unbind
  }

}
