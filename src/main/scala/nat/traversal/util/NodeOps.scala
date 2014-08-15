package nat.traversal.util

import grizzled.slf4j.Logging
import scala.xml.{Node, NodeSeq}

/**
 * Converter from `Node` to another type.
 */
trait NodeConverter[To] {

  def convert(v: Node): To

}

/**
 * Useful default `Node` converters.
 */
object NodeConverters {

  implicit val convertToString: NodeConverter[String] =
    new NodeConverter[String] {
      override def convert(v: Node): String = v.text
    }

  implicit val convertToInt: NodeConverter[Int] =
    new NodeConverter[Int] {
      override def convert(v: Node): Int = v.text.toInt
    }

  implicit val convertToBoolean: NodeConverter[Boolean] =
    new NodeConverter[Boolean] {
      override def convert(v: Node): Boolean = v.text.toLowerCase match {
        case "1" => true
        case "yes" => true
        case "true" => true
        case _ => false
      }
    }

}

/**
 * Trait defining helper functions when dealing with XML.
 */
trait NodeOps extends Logging {

  /**
   * Gets child node.
   *
   * @param node parent
   * @param childName child name to get
   * @return `Option` of requested child
   */
  protected def getChildOption(node: NodeSeq, childName: String)
    : Option[Node] =
  {
    val children = node \ childName
    children.length match {
      case 0 =>
        None

      case n =>
        if (n != 1) {
          logger.debug(s"Found $n occurrences of node[$childName] where 1 expected")
        }
        Some(children(0))
    }
  }

  /**
   * Gets child node value.
   *
   * @param node parent
   * @param childName child name to get
   * @return `Option` of requested child value
   */
  protected def getChildValueOption[T: NodeConverter]
    (node: NodeSeq, childName: String)
    : Option[T] =
  {
    val converter = implicitly[NodeConverter[T]]
    getChildOption(node, childName) map { converter.convert }
  }

  /**
   * Gets child node.
   *
   * @param node parent
   * @param childName child name to get
   * @return requested child
   */
  protected def getChild(node: NodeSeq, childName: String): Node =
    getChildOption(node, childName) getOrElse {
      throw new Error(s"Missing mandatory node[$childName]")
    }

  /**
   * Gets child node value.
   *
   * @param node parent
   * @param childName child name to get
   * @return requested child value
   */
  protected def getChildValue[T: NodeConverter]
    (node: NodeSeq, childName: String)
    : T =
  {
    val converter = implicitly[NodeConverter[T]]
    converter.convert(getChild(node, childName))
  }

}
