package utils

import java.nio.file.Path

object Implicits {

  implicit def pathToString(p: Path): String = p.toString

}
