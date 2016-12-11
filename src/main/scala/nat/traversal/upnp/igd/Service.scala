package nat.traversal.upnp.igd

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import java.net.URL
import nat.traversal.util.{NodeConverters, NodeOps}
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.xml.{Node, NodeSeq}

/**
 * Device service.
 */
class Service(val info: ServiceInfo)
  (implicit system: ActorSystem, executionContext: ExecutionContext, materializer: Materializer)
{

  val desc: ServiceDesc = info.desc

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
  def action(name: String)(args: Map[String, Any] = Map.empty): Node =
    getAction(name).get.apply(args) match {
      case Left(e) =>
        throw e

      case Right(result) =>
        (result \ "Body" \ s"${name}Response").head
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
    (implicit system: ActorSystem, executionContext: ExecutionContext, materializer: Materializer)
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
    val request = Get(SCPDURL.toString)
    val response = Http().singleRequest(request).flatMap { response =>
      Unmarshal(response.entity).to[NodeSeq]
    }
    /* XXX - timeout */
    val descNode = Await.result(
      response,
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
