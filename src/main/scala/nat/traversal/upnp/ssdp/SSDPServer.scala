package nat.traversal.upnp.ssdp

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Connection
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.server.Directives._
import akka.io.{IO, Udp}
import akka.io.Inet.{DatagramChannelCreator, SocketOptionV2}
import akka.stream.Materializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import java.net._
import java.nio.channels.DatagramChannel
import nat.traversal.upnp.UPnPManager
import nat.traversal.upnp.igd._
import nat.traversal.util._
import scala.xml._


case class RequestException(msg: String = null, cause: Throwable = null)
  extends Exception(msg, cause)

case class MockDevice(id: String)
  extends NodeOps
  with StrictLogging
{

  import NodeConverters._

  /* Note: we could reuse Service/Action/Argument classes to define a service
   * and be able to build its own description e.g. by implementing some kind
   * of Entity.toNode function.
   * But it is not worth it; would be if we were to handle a real server
   * device implementation.
   */

  private val wancServiceType = ServiceType.create("WANIPConnection", 1)

  private val startTime = System.currentTimeMillis()

  private var portMappings: List[PortMapping] = Nil

  val descRoot: String = "<?xml version=\"1.0\"?>" +
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
        <UDN>uuid:igd{id}</UDN>
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
            <UDN>uuid:wan{id}</UDN>
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
                <UDN>uuid:wanc{id}</UDN>
                <serviceList>
                  <service>
                    <serviceType>{wancServiceType.toString}</serviceType>
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

  val descWANC: String = "<?xml version=\"1.0\"?>" +
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

  private def response(serviceType: EntityType, request: String, args: Map[String, Any] = Map.empty): NodeSeq = {
    val node: NodeSeq = args.toList.map { entry =>
      val (name, value) = entry

      Option(value).map { value =>
        Elem(null, name, Null, TopScope, false, Text(value.toString))
      }.getOrElse {
        Elem(null, name, Null, TopScope, minimizeEmpty = false)
      }
    }

    wrap(serviceType, request, node)
  }

  private def wrap(serviceType: EntityType, request: String, node: NodeSeq): Elem =
    <s:Envelope
    xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
    s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
    >
      <s:Body>
        {
        Elem("u", s"${request}Response",
          new PrefixedAttribute("xmlns", "u", serviceType.toString, Null),
          TopScope, false, node: _*
        )
        }
      </s:Body>
    </s:Envelope>

  type RequestHandler = (EntityType, String, Node) => NodeSeq
  type ServiceHandler = Map[String, RequestHandler]

  private var serviceHandlers: Map[String, ServiceHandler] = Map.empty

  def addRequestHandler(serviceType: EntityType, request: String, handler: RequestHandler) {
    val service = serviceType.name
    val requestHandlers = serviceHandlers.getOrElse(service, Map.empty) +
      (request -> handler)
    serviceHandlers += (service -> requestHandlers)
  }

  def handleRequest(serviceType: EntityType, request: String, node: Node): NodeSeq = {
    val service = serviceType.name
    serviceHandlers.get(service).map { handler =>
      handler.get(request).map { handler =>
        handler(serviceType, request, node)
      }.getOrElse {
        logger.error(s"Unhandled service[$service] action[$request]")
        throw RequestException()
      }
    }.getOrElse {
      logger.error(s"Unhandled service[$service]")
      throw RequestException()
    }
  }

  private def requestWANCGetStatusInfo(serviceType: EntityType, request: String, node: Node): NodeSeq = {
    val args = Map[String, Any](
      "NewConnectionStatus" -> ConnectionStatus.Connected,
      "NewLastConnectionError" -> "ERROR_NONE",
      "NewUptime" -> (System.currentTimeMillis() - startTime) / 1000
    )
    response(serviceType, request, args)
  }

  private def requestWANCGetExternalIPAddress(serviceType: EntityType, request: String, node: Node): NodeSeq = {
    val args = Map[String, Any](
      "NewExternalIPAddress" -> "1.1.1.1"
    )
    response(serviceType, request, args)
  }

  private def requestWANCGetGenericPortMappingEntry(serviceType: EntityType, request: String, node: Node): NodeSeq = {
    /* XXX - handle real UPnP error (check specs), e.g.
  <?xml version="1.0"?>
  <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
      <s:Fault>
        <faultcode>s:Client</faultcode>
        <faultstring>UPnPError</faultstring>
        <detail>
          <UPnPError xmlns="urn:schemas-upnp-org:control-1-0">
            <errorCode>713</errorCode>
            <errorDescription>Bad Array Index</errorDescription>
          </UPnPError>
        </detail>
      </s:Fault>
    </s:Body>
  </s:Envelope>
     */

    val idx = getChildValue[Int](node, "NewPortMappingIndex")
    val mapping = portMappings.lift(idx).getOrElse {
      /* Out of bound */
      throw RequestException()
    }

    val args = Map[String, Any](
      "NewRemoteHost" -> mapping.remoteHost,
      "NewExternalPort" -> mapping.externalPort,
      "NewProtocol" -> mapping.protocol,
      "NewInternalPort" -> mapping.internalPort,
      "NewInternalClient" -> mapping.internalClient,
      "NewEnabled" -> mapping.enabled,
      "NewPortMappingDescription" -> mapping.description,
      "NewLeaseDuration" -> mapping.leaseDuration
    )
    response(serviceType, request, args)
  }

  private def removePortMapping(mapping: PortMapping): Boolean = {
    val (found, mappings) =
      portMappings.foldLeft(false, List[PortMapping]()) { (acc, m) =>
        val (found, mappings) = acc
        val found2 = (m.remoteHost == mapping.remoteHost) &&
          (m.externalPort == mapping.externalPort) &&
          (m.protocol == mapping.protocol)
        if (found2) (found2, mappings)
        else (found, mappings :+ m)
      }

    if (found) portMappings = mappings
    found
  }

  private def requestWANCAddPortMapping(serviceType: EntityType, request: String, node: Node): NodeSeq = {
    val mapping = PortMapping(node)
    removePortMapping(mapping)
    portMappings :+= mapping
    response(serviceType, request)
  }

  private def requestWANCDeletePortMapping(serviceType: EntityType, request: String, node: Node): NodeSeq = {
    /* Note: delete request only contains external information */
    val mapping = PortMapping(
      remoteHost = getChildValue[String](node, "NewRemoteHost"),
      externalPort = getChildValue[Int](node, "NewExternalPort"),
      protocol = Protocol.withName(getChildValue[String](node, "NewProtocol")),
      internalPort = -1,
      internalClient = "",
      enabled = true,
      description = "",
      leaseDuration = 0
    )

    removePortMapping(mapping)
    response(serviceType, request)
  }

  addRequestHandler(wancServiceType, "GetStatusInfo", requestWANCGetStatusInfo)
  addRequestHandler(wancServiceType, "GetExternalIPAddress", requestWANCGetExternalIPAddress)
  addRequestHandler(wancServiceType, "GetGenericPortMappingEntry", requestWANCGetGenericPortMappingEntry)
  addRequestHandler(wancServiceType, "AddPortMapping", requestWANCAddPortMapping)
  addRequestHandler(wancServiceType, "DeletePortMapping", requestWANCDeletePortMapping)

}

/* Our service behavior, defined independently from the service actor.
 * Allows separate testing without having to create a whole system.
 */
trait SSDPServerHttp
  extends NodeOps
  with StrictLogging
{

  val device = MockDevice("01234567-0123-0123-0123-0123456789ab")

  val exceptionHandler = ExceptionHandler {
    case _: RequestException =>
      respondWithHeader(Connection("close")) {
        complete(StatusCodes.BadRequest)
      }
  }

  private def xmlResponse(body: String): HttpEntity.Strict = {
    HttpEntity(ContentTypes.`text/xml(UTF-8)`, body)
  }

  val myRoute: Route =
    /* XXX - use device UUID as path prefix (would allow to have more than one mock device) */
    extractUri { uri =>
      path("desc" / "root") {
        get {
          respondWithHeader(Connection("close")) {
            complete {
              xmlResponse(device.descRoot)
            }
          }
        }
      } ~
      path("desc" / "wan_ip_connection") {
        get {
          respondWithHeader(Connection("close")) {
            complete {
              xmlResponse(device.descWANC)
            }
          }
        }
      } ~
      path("control" / "wan_ip_connection") {
        post {
          optionalHeaderValueByName("SOAPACTION") { soapAction =>
            soapAction.map { soapAction =>
              soapAction.split("#").toList match {
                case service :: request :: Nil =>
                  ServiceType.get(service) match {
                    case Some(serviceType) =>
                      entity(as[NodeSeq]) { node =>
                        /* XXX - upon failure, getChild throws Error; switch to our own Exception ? */
                        getChildOption(node, "Body").flatMap { body =>
                          getChildOption(body, request)
                        }.fold {
                          logger.error(s"Posted SOAP action[$soapAction] body is malformed for path[${uri.path}]")
                          failWith(RequestException())
                        } { requestNode =>
                          complete {
                            device.handleRequest(serviceType, request, requestNode)
                          }
                        }
                      }

                    case None =>
                      logger.error(s"Posted SOAP action service[$service] is malformed for path[${uri.path}]")
                      failWith(RequestException())
                  }

                case _ =>
                  logger.error(s"Posted SOAPACTION[$soapAction] header is not of the form ServicePath#ActionName for path[${uri.path}]")
                  failWith(RequestException())
              }
            }.getOrElse {
              logger.error(s"Missing SOAPACTION header for path[${uri.path}]")
              failWith(RequestException())
            }
          }
        }
      }
    }

}

class SSDPServerService(implicit system: ActorSystem, materializer: Materializer)
  extends Actor
  with SSDPServerHttp
{

  val route: Route = handleExceptions(exceptionHandler) {
    myRoute
  }

  // We shamelessly handle Udp directly in the actor and Http through its route
  Http().bindAndHandle(route, interface = "0.0.0.0", port = 5678)

  def receive: Receive = {
    case Udp.Bound(_) =>
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      /* XXX - use interface IP, and HTTP server port */
      HTTPParser(data.utf8String) match {
        case Left(failure) =>
          logger.error(s"SSDP server received invalid message from ${remote.getAddress}: message[${data.utf8String}] error[$failure]")

        case Right(httpMsg) =>
          logger.debug(s"SSDP server received message from ${remote.getAddress}: ${httpMsg.headers}")
          httpMsg match {
            case request: RFC2616.Request =>
              if ((request.method == "M-SEARCH") &&
                (request.requestURI == "*") &&
                (request.headers.getOrElse("St", "") == "urn:schemas-upnp-org:device:InternetGatewayDevice:1")
              ) {
                /* XXX - move up to share with others */
                val conf = ConfigFactory.load()
                val serverDesc = conf.getString("akka.http.server.server-header")
                val msg =
                  "HTTP/1.1 200 OK\r\n" +
                  "SERVER: " + serverDesc + "\r\n" +
                  "LOCATION: http://192.168.0.16:5678/desc/root\r\n" +
                  "EXT:\r\n" +
                  "CACHE-CONTROL: max-age=1800\r\n" +
                  "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n" +
                  "USN: uuid:igd01234567-0123-0123-0123-0123456789ab::urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n" +
                  "\r\n"
                socket ! Udp.Send(ByteString(msg, "US-ASCII"), remote)
              }

            case _ =>
              /* XXX */
          }
      }

    case Udp.Unbind =>
      socket ! Udp.Unbind
      // XXX - stop Http service too

    case Udp.Unbound =>
      // XXX - only stop once Http down too ?
      context.stop(self)
  }

}

object SSDPServerService {

  import UPnPManager.system
  import UPnPManager.materializer

  val ssdpServerService: ActorRef = system.actorOf(props, "upnp-service-ssdp-server")

  def start() {
    // The 'Bound' response is sent to the sender of the 'Bind' request
    implicit val sender = ssdpServerService

    // For UDP Multicast support, see: http://doc.akka.io/docs/akka/2.4.14/scala/io-udp.html#UDP_Multicast
    SSDPClientService.getNetworkInterfaces.headOption.foreach { interface =>
      val opts = List(InetProtocolFamily(), MulticastGroup("239.255.255.250", interface))
      IO(Udp) ! Udp.Bind(ssdpServerService, new InetSocketAddress(1900), opts)
    }
  }

  def stop() {
    ssdpServerService ! Udp.Unbind
  }

  final case class InetProtocolFamily() extends DatagramChannelCreator {
    override def create(): DatagramChannel =
      DatagramChannel.open(StandardProtocolFamily.INET)
  }

  final case class MulticastGroup(address: String, interface: NetworkInterface) extends SocketOptionV2 {
    override def afterBind(s: DatagramSocket) {
      val group = InetAddress.getByName(address)
      s.getChannel.join(group, interface)
      ()
    }
  }

  def props(implicit system: ActorSystem, materializer: Materializer): Props = {
    Props(new SSDPServerService)
  }

}
