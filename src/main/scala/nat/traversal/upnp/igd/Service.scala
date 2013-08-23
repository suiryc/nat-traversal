package nat.traversal.upnp.igd

import akka.actor.ActorRefFactory
import java.net.URL
import nat.traversal.util.{NodeConverters, NodeOps}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.xml.NodeSeq
import spray.client.pipelining._
import spray.http.HttpRequest

/**
 * Device service.
 */
class Service(val info: ServiceInfo)
  (implicit refFactory: ActorRefFactory, executionContext: ExecutionContext)
{

  val desc = info.desc

  val actionList: List[Action] =
    desc.actionList.map { actionInfo =>
      new Action(this, actionInfo)
    }

  /**
   * Gets action by name.
   *
   * @param name action name
   * @return corresponding action
   */
  def getAction(name: String): Option[Action] =
    actionList.find { action => action.info.name == name }

  /**
   * Performs action, and returns response node.
   *
   * @param name action name
   * @param args action arguments
   * @return action response node
   */
  def action(name: String)(args: Map[String, Any] = Map.empty) =
    getAction(name).get.apply(args) match {
      case Left(e) =>
        throw e

      case Right(result) =>
        (result \ "Body" \ s"${name}Response")(0)
    }

}

/**
 * Device service info.
 *
 * As found in the root device description URL.
 */
class ServiceInfo(
  val serviceType: EntityType,
  val serviceId: String,
  val SCPDURL: URL,
  val controlURL: URL,
  val eventSubURL: URL,
  val desc: ServiceDesc
) extends Entity
{

  override def toString: String =
    "serviceType[" + serviceType +
      "] serviceId[" + serviceId +
      "] SCPDURL[" + SCPDURL +
      "] desc[" + desc +
      "]"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "serviceType", serviceType)
    prettyString(builder, level, "serviceId", serviceId)
    prettyString(builder, level, "SCPDURL", SCPDURL)
    prettyString(builder, level, "controlURL", controlURL)
    prettyString(builder, level, "eventSubURL", eventSubURL)
    prettyString(builder, level, "desc", desc)
  }

}

/** `EntityType` builder dedicated to services. */
object ServiceType extends EntityTypeBuilder("service")

object ServiceInfo extends NodeOps {

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
    : ServiceInfo =
  {
    val serviceType: EntityType = ServiceType(
      getChild(node, "serviceType").text
    )
    val serviceId: String =
      getChildValue[String](node, "serviceId")
    val SCPDURL: URL =
      new URL(base, getChildValue[String](node, "SCPDURL"))
    val controlURL: URL =
      new URL(base, getChildValue[String](node, "controlURL"))
    val eventSubURL: URL =
      new URL(base, getChildValue[String](node, "eventSubURL"))
    val pipeline: HttpRequest => Future[NodeSeq] = (
      sendReceive
      ~> unmarshal[NodeSeq]
    )
    /* XXX - timeout */
    val descNode = Await.result(
      pipeline(Get(SCPDURL.toString)),
      Duration.Inf
    )
    val desc: ServiceDesc = ServiceDesc(descNode)

    new ServiceInfo(
      serviceType = serviceType,
      serviceId = serviceId,
      SCPDURL = SCPDURL,
      controlURL = controlURL,
      eventSubURL = eventSubURL,
      desc = desc
    )
  }

}

/**
 * Service description.
 *
 * As found in the Service Control Protocol Definition URL.
 */
class ServiceDesc(
  val specVersion: SpecVersion,
  val actionList: List[ActionInfo],
  val serviceStateTable: List[StateVariable]
) extends Entity
{

  override def toString: String =
    "specVersion[" + specVersion +
    "] actionList[" + actionList +
    "] serviceStateTable[" + serviceStateTable +
    "]"

  override def prettyString(builder: StringBuilder, level: Int): StringBuilder =
  {
    prettyString(builder, level, "specVersion", specVersion)
    prettyString(builder, level, "actionList", actionList)
    prettyString(builder, level, "serviceStateTable", serviceStateTable)
  }

}

object ServiceDesc extends NodeOps {

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @return corresponding instance
   */
  def apply(node: NodeSeq): ServiceDesc = {
    val specVersion: SpecVersion = SpecVersion(
      getChild(node, "specVersion")
    )
    val actionList: List[ActionInfo] =
      (getChild(node, "actionList") \ "action").toList.
      map(node => ActionInfo(node))
    val serviceStateTable: List[StateVariable] =
      (getChild(node, "serviceStateTable") \ "stateVariable").toList.
      map(node => StateVariable(node))

    new ServiceDesc(
      specVersion = specVersion,
      actionList = actionList,
      serviceStateTable = serviceStateTable
    )
  }

}
