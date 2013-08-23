package nat.traversal.upnp.igd

import java.net.URL
import nat.traversal.util.{NodeConverters, NodeOps}
import scala.xml.NodeSeq

/**
 * Device icon.
 *
 * As found in the root device description URL.
 */
class Icon(
  val mimetype: String,
  val width: Int,
  val height: Int,
  val depth: Int,
  val url: URL
) extends Entity
{

  override def toString: String = s"(${mimetype})${url}"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "mimetype", mimetype)
    prettyString(builder, level, "width", width)
    prettyString(builder, level, "height", height)
    prettyString(builder, level, "depth", depth)
    prettyString(builder, level, "url", url)
  }

}

object Icon extends NodeOps {

  import NodeConverters._

  /**
   * Creates an instance from the corresponding XML node.
   *
   * @param node entity XML node
   * @param base root of relative URLs
   * @return corresponding instance
   */
  def apply(node: NodeSeq, base: URL): Icon = {
    val mimetype: String = getChildValue[String](node, "mimetype")
    val width: Int = getChildValue[Int](node, "width")
    val height: Int = getChildValue[Int](node, "height")
    val depth: Int = getChildValue[Int](node, "depth")
    val url: URL = new URL(base, getChildValue[String](node, "url"))

    new Icon(
      mimetype = mimetype,
      width = width,
      height = height,
      depth = depth,
      url = url
    )
  }

}
