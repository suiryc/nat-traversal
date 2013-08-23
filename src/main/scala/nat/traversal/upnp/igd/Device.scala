package nat.traversal.upnp.igd

import akka.actor.ActorRefFactory
import java.net.{InetSocketAddress, URL}
import nat.traversal.util.{NodeConverters, NodeOps}
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

/**
 * Generic device.
 */
class Device(
  val desc: DeviceDesc,
  val localAddress: InetSocketAddress
)(implicit refFactory: ActorRefFactory, executionContext: ExecutionContext)
  extends NodeOps
{

  import NodeConverters._

  val serviceList: List[Service] = desc.serviceList.map { serviceInfo =>
    new Service(serviceInfo)
  }

  val deviceList: List[Device] = desc.deviceList.map { deviceDesc =>
    new Device(deviceDesc, localAddress)
  }

  val linkProperties: Option[DeviceLinkProperties] =
    if (desc.deviceType.name == "WANDevice") {
      /* A WANDevice has a WANCommonInterfaceConfig device.
       * A WANCommonInterfaceConfig has a GetCommonLinkProperties service.
       */
      try {
        (for (wanCIF <- getService("WANCommonInterfaceConfig"))
          yield wanCIF.action("GetCommonLinkProperties")()) map { result =>
            new DeviceLinkProperties(
              getChildValue[String](result, "NewWANAccessType"),
              getChildValue[Int](result, "NewLayer1UpstreamMaxBitRate"),
              getChildValue[Int](result, "NewLayer1DownstreamMaxBitRate")
            )
          }
      }
      catch {
        case e: Throwable =>
          None
      }
    } else None

  /**
   * Gets services by name.
   *
   * @param name service name
   * @return corresponding services
   */
  def getServices(name: String): List[Service] =
    serviceList.filter { service => service.info.serviceType.name == name }

  /**
   * Gets first service by name.
   *
   * @param name service name
   * @return corresponding service
   */
  def getService(name: String): Option[Service] =
    getServices(name) headOption

  /**
   * Gets devices by name.
   *
   * @param name device name
   * @return corresponding devices
   */
  def getDevices(name: String): List[Device] =
    deviceList.filter { device => device.desc.deviceType.name == name }

  /**
   * Gets first device by name.
   *
   * @param name device name
   * @return corresponding device
   */
  def getDevice(name: String): Option[Device] =
    getDevices(name) headOption

}

/** `EntityType` builder dedicated to devices. */
object DeviceType extends EntityTypeBuilder("device")

/**
 * Device description.
 *
 * As found in the root device description URL.
 */
class DeviceDesc(
  val deviceType: EntityType,
  val friendlyName: String,
  val manufacturer: String,
  val manufacturerURL: Option[URL],
  val modelDescription: Option[String],
  val modelName: String,
  val modelNumber: Option[String],
  val modelURL: Option[URL],
  val serialNumber: Option[String],
  val UDN: String,
  val UPC: Option[String],
  val iconList: List[Icon],
  val serviceList: List[ServiceInfo],
  val deviceList: List[DeviceDesc],
  val presentationURL: Option[URL]
) extends Entity
{

  override def toString: String =
    "deviceType[" + deviceType +
      "] friendlyName[" + friendlyName +
      "] manufacturer[" + manufacturer +
      "] modelName[" + modelName +
      "] UDN[" + UDN +
      "] iconList[" + iconList +
      "] serviceList[" + serviceList +
      "] deviceList[" + deviceList +
      "]"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "deviceType", deviceType)
    prettyString(builder, level, "friendlyName", friendlyName)
    prettyString(builder, level, "manufacturer", manufacturer)
    prettyString(builder, level, "manufacturerURL", manufacturerURL)
    prettyString(builder, level, "modelDescription", modelDescription)
    prettyString(builder, level, "modelName", modelName)
    prettyString(builder, level, "modelNumber", modelNumber)
    prettyString(builder, level, "modelURL", modelURL)
    prettyString(builder, level, "serialNumber", serialNumber)
    prettyString(builder, level, "UDN", UDN)
    prettyString(builder, level, "UPC", UPC)
    prettyString(builder, level, "iconList", iconList)
    prettyString(builder, level, "serviceList", serviceList)
    prettyString(builder, level, "deviceList", deviceList)
    prettyString(builder, level, "presentationURL", presentationURL)
  }

}

object DeviceDesc extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @param base root of relative URLs
   * @return corresponding instance
   */
  def apply(node: NodeSeq, base: URL)
    (implicit refFactory: ActorRefFactory, executionContext: ExecutionContext)
    : DeviceDesc =
  {
    val deviceType: EntityType = DeviceType(
      getChild(node, "deviceType").text
    )
    val friendlyName: String =
      getChildValue[String](node, "friendlyName")
    val manufacturer: String =
      getChildValue[String](node, "manufacturer")
    val manufacturerURL: Option[URL] =
      getChildOption(node, "manufacturerURL").
      map(node => new URL(base, node.text))
    val modelDescription: Option[String] =
      getChildValueOption[String](node, "modelDescription")
    val modelName: String =
      getChildValue[String](node, "modelName")
    val modelNumber: Option[String] =
      getChildValueOption[String](node, "modelNumber")
    val modelURL: Option[URL] =
      getChildOption(node, "modelURL").
      map(node => new URL(base, node.text))
    val serialNumber: Option[String] =
      getChildValueOption[String](node, "serialNumber")
    val UDN: String =
      getChildValue[String](node, "UDN")
    val UPC: Option[String] =
      getChildValueOption[String](node, "UPC")
    val iconList: List[Icon] =
      (getChildOption(node, "iconList").
      getOrElse(NodeSeq.Empty) \ "icon").toList.
      map(node => Icon(node, base))
    val serviceList: List[ServiceInfo] =
      (getChildOption(node, "serviceList").
      getOrElse(NodeSeq.Empty) \ "service").toList.
      map(node => ServiceInfo(node, base))
    val deviceList: List[DeviceDesc] =
      (getChildOption(node, "deviceList").
      getOrElse(NodeSeq.Empty) \ "device").toList.
      map(node => DeviceDesc(node, base))
    val presentationURL: Option[URL] =
      getChildOption(node, "presentationURL").
      map(node => new URL(base, node.text))

    new DeviceDesc(
      deviceType = deviceType,
      friendlyName = friendlyName,
      manufacturer = manufacturer,
      manufacturerURL = manufacturerURL,
      modelDescription = modelDescription,
      modelName = modelName,
      modelNumber = modelNumber,
      modelURL = modelURL,
      serialNumber = serialNumber,
      UDN = UDN,
      UPC = UPC,
      iconList = iconList,
      serviceList = serviceList,
      deviceList = deviceList,
      presentationURL = presentationURL
    )
  }

}

class DeviceLinkProperties(
  val accessType: String,
  val upstreamMaxBitRate: Int,
  val downstreamMaxBitRate: Int
)
