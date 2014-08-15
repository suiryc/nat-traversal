package nat.traversal.upnp.igd

import nat.traversal.util.{NodeConverters, NodeOps}
import scala.xml.NodeSeq

/**
 * Spec version.
 *
 * As found in device/service description URLs.
 */
class SpecVersion(
  val major: Int,
  val minor: Int
) extends Entity
{

  override def toString: String = s"$major.$minor"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "major", major)
    prettyString(builder, level, "minor", minor)
  }

}

object SpecVersion extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @return corresponding instance
   */
  def apply(node: NodeSeq): SpecVersion = {
    val major = getChildValue[Int](node, "major")
    val minor = getChildValue[Int](node, "minor")

    new SpecVersion(
      major = major,
      minor = minor
    )
  }

}
