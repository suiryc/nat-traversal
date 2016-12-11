package nat.traversal.upnp.igd

import scala.util.matching.Regex

/**
 * Common trait for IGD SDCP entities.
 *
 * Its main purpose is to offer a generic way to format the entity to print its
 * content in a human-readable way.
 */
trait Entity {

  /**
   * Each entity needs to implement this function by processing its fields.
   *
   * For each field, it calls `prettyString` giving the key and value.
   *
   * @param builder where to format the entity content
   * @param level indent (that is depth) level
   * @return the builder
   */
  def prettyString(builder: StringBuilder, level: Int): StringBuilder

  /**
   * Formats a "key: value" pair.
   *
   * Adds a new line to the builder, with recursive handling if the given value
   * is an entity itself.
   *
   * @param builder where to format the entity content
   * @param level indent (that is depth) level
   * @param key the field key
   * @param value the field value
   * @return the builder
   */
  def prettyString(builder: StringBuilder, level: Int, key: String, value: Any)
    : StringBuilder =
  {
    if (level > 0) {
      builder.append(" " * (level * 2))
    }
    builder.append(key)
      .append(":")
    value match {
      case entity: Entity =>
        builder.append("\n")
        entity.prettyString(builder, level + 1)

      case list: List[_] if list.isEmpty =>
        builder.append(" None\n")

      case list: List[_] =>
        builder.append("\n")
        @annotation.tailrec
        def loop(level: Int, index: Int, list: List[Any])
          : StringBuilder =
        {
          list match {
            case head :: tail =>
              prettyString(builder, level, "#" + index, head)
              loop(level, index + 1, tail)

            case Nil =>
              builder
          }
        }
        loop(level + 1, 1, list)

      case _ =>
        builder.append(" ")
          .append(value)
          .append("\n")
    }
  }

}

/**
 * An entity type.
 *
 * `prefix:name:version` in the specs.
 */
case class EntityType(
  prefix: String,
  name: String,
  version: Int
) extends Entity
{

  override def toString: String =
    s"$prefix:$name:$version"

  override def prettyString(builder: StringBuilder, level: Int)
    : StringBuilder =
  {
    prettyString(builder, level, "prefix", prefix)
    prettyString(builder, level, "name", name)
    prettyString(builder, level, "version", version)
  }

}

/**
 * Helper class to build entity types.
 *
 * Prefix is of the form `urn:schemas-upnp-org:type` where `type` is the type of
 * entity: `device` or `service`.
 */
class EntityTypeBuilder(entity: String) {

  val prefix = s"urn:schemas-upnp-org:$entity"

  /** Pattern to split entity type value into prefix, name and version. */
  protected val pattern: Regex =
    ("^(" + prefix + """):(\p{Alnum}+):([0-9]+)$""").r

  /**
   * Creates a new entity type corresponding to the given value.
   *
   * @param value entity type
   * @return entity type object
   */
  def apply(value: String): EntityType =
    get(value).get

  /**
   * Creates a new entity type corresponding to the given value.
   *
   * @param value entity type
   * @return Some entity type object, or None
   */
  def get(value: String): Option[EntityType] =
    value match {
      case pattern(prefix, name, version) =>
        Some(EntityType(
          prefix = prefix,
          name = name,
          version = version.toInt
        ))

      case _ =>
        None
    }

  /**
   * Creates a new entity type corresponding to the given values.
   *
   * @param name entity name
   * @param version entity version
   * @return entity type object
   */
  def create(name: String, version: Int) =
    EntityType(
      prefix = prefix,
      name = name,
      version = version.toInt
    )

}
