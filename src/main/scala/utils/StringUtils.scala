package utils

object StringUtils extends App {

  def escape(s: String): String = s.flatMap(escapedChar)

  def escapedChar(ch: Char): String = ch match {
    case '\b' => "\\b"
    case '\t' => "\\t"
    case '\n' => "\\n"
    case '\f' => "\\f"
    case '\r' => "\\r"
    case '"' => "\\\""
    case '\'' => "\\\'"
    case '\\' => "\\\\"
    case _ => if (ch.isControl) "\\0" + Integer.toOctalString(ch.toInt)
    else String.valueOf(ch)
  }

}
