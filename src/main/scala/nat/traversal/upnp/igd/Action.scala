package nat.traversal.upnp.igd

import akka.actor.ActorRefFactory
import nat.traversal.util.{NodeConverters, NodeOps}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.xml.{Elem, NodeSeq, Null, PrefixedAttribute, Text, TopScope}
import spray.client.pipelining._
import spray.http.HttpRequest

/**
 * Service action.
 */
class Action(
  val service: Service,
  val info: ActionInfo
)
{

  /**
   * Performs action.
   *
   * @param args action arguments
   * @return response, or error
   */
  def apply(args: Map[String, Any] = Map.empty)
    (implicit refFactory: ActorRefFactory, executionContext: ExecutionContext)
    : Either[Throwable, NodeSeq] =
  {
    val node: NodeSeq = info.argumentList.filter { argument =>
      argument.direction == ArgumentDirection.in
    }.map { argument =>
      args.get(argument.name).map { value =>
        Elem(null, argument.name, Null, TopScope, false, Text(value.toString))
      }.getOrElse(Elem(null, argument.name, Null, TopScope, minimizeEmpty = false))
    }

    val request = wrap(node)
    /* If we had the string representation of an XML content, we could define
     * this implicit Marshaller delegation to set the correct Content-Type
     * (instead of the default 'text/plain'):
     *
     * implicit val XMLStringMarshaller =
     *   Marshaller.delegate[String, String](MediaTypes.`text/xml`) { x => x }
     */
    val pipeline: HttpRequest => Future[NodeSeq] = (
      addHeader("SOAPACTION", s"${service.info.serviceType}#${info.name}")
      ~> sendReceive
      ~> unmarshal[NodeSeq]
    )

    try {
      Right(
        Await.result(
          pipeline(Post(service.info.controlURL.toString, request)),
          /* XXX - timeout */
          Duration.Inf
        )
      )
    }
    catch {
      case e: Throwable =>
        Left(e)
    }
  }

  /**
   * Builds SOAP request content.
   */
  private def wrap(node: NodeSeq): Elem =
    <s:Envelope
      xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
      s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
    >
      <s:Body>
        {
          Elem("u", info.name,
            new PrefixedAttribute("xmlns", "u", service.info.serviceType.toString, Null),
            TopScope, false, node: _*
          ) 
        }
     </s:Body>
    </s:Envelope>

}

/**
 * Service action info.
 *
 * As found in the Service Control Protocol Definition URL.
 */
class ActionInfo(
  val name: String,
  val argumentList: List[Argument]
) extends Entity
{

  override def toString: String = name

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "name", name)
    prettyString(builder, level, "argumentList", argumentList)
  }

}

object ActionInfo extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @return corresponding instance
   */
  def apply(node: NodeSeq): ActionInfo = {
    val name: String = getChildValue[String](node, "name")
    val argumentList: List[Argument] =
      (getChildOption(node, "argumentList").
        getOrElse(NodeSeq.Empty) \ "argument").toList.map
      { node => Argument(node) }

    new ActionInfo(
      name = name,
      argumentList = argumentList
    )
  }

}
