package nat.traversal.util

import scala.util.parsing.combinator._

/* http://stackoverflow.com/questions/9194445/how-to-parse-line-based-text-file-mht-in-scala-way */

object RFC2616 extends RFC2616 {

  class Message(val headers: Map[String, String], val text: String)

  class Request(
    val method: String,
    val requestURI: String,
    override val headers: Map[String, String],
    override val text: String
  ) extends Message(headers, text)

  class Response(
    val statusCode: Int,
    val reason: String,
    override val headers: Map[String, String],
    override val text: String
  ) extends Message(headers, text)

}

class RFC2616 extends RegexParsers {

  override def skipWhitespace = false

  def message =
    requestLine ~ (headers <~ CRLF) ~ text ^^ {
      case (method ~ _ ~ requestURI ~ _ ~ httpVersion) ~ hd ~ txt =>
        new RFC2616.Request(method, requestURI, hd, txt)
    } | statusLine ~ (headers <~ CRLF) ~ text ^^ {
      case (httpVersion ~ _ ~ statusCode ~ _ ~ reasonPhrase) ~ hd ~ txt =>
        new RFC2616.Response(Integer.valueOf(statusCode), reasonPhrase, hd, txt)
    }

  def startLine = requestLine | statusLine

  /* Note: 'method' is case-sensitive */
  def requestLine = method ~ SP ~ requestURI ~ SP ~ httpVersion <~ CRLF

  def statusLine = httpVersion ~ SP ~ statusCode ~ SP ~ reasonPhrase <~ CRLF

  def method = """[^:\P{Graph}]+""".r

  def requestURI = "*" | """\S+""".r

  def httpVersion = """HTTP/\d+\.\d+""".r

  def statusCode = """\d{3}""".r

  def reasonPhrase = ".*".r

  def headers = field.* ^^ { _.toMap }

  /* Note: field name is case-insensitive */
  def field = (fieldName <~ LWSPChar.* ~ ":") ~
    (LWSPChar.* ~> fieldBody <~ LWSPChar.* ~ CRLF) ^^
  {
    case name ~ body =>
      /* Capitalize first letter of each word, and lowercase everything else.
       * Ex: 'aBc-DeF' becomes 'Abc-Def'
       */
      val cleanedName = name.split("-").map { word =>
        word.toLowerCase.capitalize
      }.mkString("-")
      cleanedName -> body
  }

  def fieldName = """[^:\P{Graph}]+""".r

  def fieldBody: Parser[String] = fieldBodyContents ~
    (CRLF ~> LWSPChar.+ ~> fieldBody).? ^^
  {
    case a ~ Some(b) => a + SP + b
    case a ~ None    => a
  }

  def fieldBodyContents = ".*".r

  def SP = " "
 
  def CRLF = """\r?\n""".r

  def LWSPChar = SP | "\t"

  def text = "(?s).*".r

}

object HTTPParser {

  import RFC2616._

  def apply(httpMsg: String): Either[ParseResult[Message], Message] = {
    import RFC2616._

    parseAll(message, httpMsg) match {
      case Success(result, _) =>
        Right(result)

      case failure =>
        Left(failure)
    }
  }

}
