package nat.traversal.upnp.igd

import nat.traversal.util.{NodeConverters, NodeOps}
import scala.xml.NodeSeq

/**
 * Service state variable.
 *
 * As found in the Service Control Protocol Definition URL.
 */
class StateVariable(
  val name: String,
  val sendEvents: Boolean,
  val dataType: String,
  val defaultValue: Option[String],
  val allowedValueList: List[String],
  val allowedValueRange: Option[AllowedValueRange]
) extends Entity
{

  override def toString = name

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "name", name)
    prettyString(builder, level, "sendEvents", sendEvents)
    prettyString(builder, level, "dataType", dataType)
    prettyString(builder, level, "defaultValue", defaultValue)
    prettyString(builder, level, "allowedValueList", allowedValueList)
    prettyString(builder, level, "allowedValueRange", allowedValueRange)
  }

}

object StateVariable extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @return corresponding instance
   */
  def apply(node: NodeSeq): StateVariable = {
    val name: String =
      getChildValue[String](node, "name")
    val sendEvents: Boolean =
      getChildValueOption[String](node, "@sendEvents").
      getOrElse("yes").toLowerCase match
    {
      case "yes" => true
      case _     => false
    }
    val dataType: String =
      getChildValue[String](node, "dataType")
    val defaultValue: Option[String] =
      getChildValueOption[String](node, "defaultValue")
    val allowedValueList: List[String] =
      (getChildOption(node, "allowedValueList").
      getOrElse(NodeSeq.Empty) \ "allowedValue").toList.
      map(node => node.text)
    val allowedValueRange: Option[AllowedValueRange] =
      getChildOption(node, "allowedValueRange").
      map(node => AllowedValueRange(node))

    new StateVariable(
      name = name,
      sendEvents = sendEvents,
      dataType = dataType,
      defaultValue = defaultValue,
      allowedValueList = allowedValueList,
      allowedValueRange = allowedValueRange
    )
  }

}

/**
 * Variable allowed value range.
 *
 * As found in the Service Control Protocol Definition URL.
 */
class AllowedValueRange(
  val minimum: String,
  val maximum: String
) extends Entity
{

  override def toString: String = s"[${minimum},${maximum}]"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "minimum", minimum)
    prettyString(builder, level, "maximum", maximum)
  }

}

object AllowedValueRange extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @return corresponding instance
   */
  def apply(node: NodeSeq): AllowedValueRange = {
    val minimum: String = getChildValue[String](node, "minimum")
    val maximum: String = getChildValue[String](node, "maximum")

    new AllowedValueRange(
      minimum = minimum,
      maximum = maximum
    )
  }

}
