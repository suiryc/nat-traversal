package nat.traversal.upnp.igd

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import nat.traversal.util.{NodeConverters, NodeOps}
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.xml.{Elem, NodeSeq, Null, PrefixedAttribute, Text, TopScope}

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
    (implicit system: ActorSystem, executionContext: ExecutionContext, materializer: Materializer)
    : Either[Throwable, NodeSeq] =
  {
    val node: NodeSeq = info.argumentList.filter { argument =>
      argument.direction == ArgumentDirection.in
    }.map { argument =>
      args.get(argument.name).map { value =>
        Elem(null, argument.name, Null, TopScope, false, Text(value.toString))
      }.getOrElse(Elem(null, argument.name, Null, TopScope, minimizeEmpty = false))
    }

    val requestBody = wrap(node)
    /* (legacy note with spray implementation)
     * If we had the string representation of an XML content, we could define
     * this implicit Marshaller delegation to set the correct Content-Type
     * (instead of the default 'text/plain'):
     *
     * implicit val XMLStringMarshaller =
     *   Marshaller.delegate[String, String](MediaTypes.`text/xml`) { x => x }
     */
    val request = Post(service.info.controlURL.toString, requestBody).addHeader(RawHeader("SOAPACTION", s"${service.info.serviceType}#${info.name}"))
    val response = Http().singleRequest(request).flatMap { response =>
      Unmarshal(response.entity).to[NodeSeq]
    }

    try {
      Right(
        Await.result(
          response,
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
