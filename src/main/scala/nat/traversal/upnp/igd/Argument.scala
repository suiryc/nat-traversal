package nat.traversal.upnp.igd

import nat.traversal.util.{NodeConverters, NodeOps}
import scala.xml.NodeSeq

object ArgumentDirection extends Enumeration {

  /** Input argument */
  val in = Value
  /** Output argument */
  val out = Value

}

/**
 * Service action argument.
 *
 * As found in the Service Control Protocol Definition URL.
 */
class Argument(
  val name: String,
  val direction: ArgumentDirection.Value,
  val relatedStateVariable: String
) extends Entity
{

  override def toString: String = s"(${direction})${name}"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "name", name)
    prettyString(builder, level, "direction", direction)
    prettyString(builder, level, "relatedStateVariable", relatedStateVariable)
  }

}

object Argument extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @return corresponding instance
   */
  def apply(node: NodeSeq): Argument = {
    val name: String = getChild(node, "name").text
    val direction: ArgumentDirection.Value = ArgumentDirection.withName(
      getChild(node, "direction").text.toLowerCase
    )
    val relatedStateVariable: String =
      getChildValue[String](node, "relatedStateVariable")

    new Argument(
      name = name,
      direction = direction,
      relatedStateVariable = relatedStateVariable
    )
  }

}
