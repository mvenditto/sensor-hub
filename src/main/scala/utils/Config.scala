package utils

import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

object Config extends App {

  protected implicit def camelCaseHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  case class X(x: String)
  val cfg = loadConfig[X](ConfigFactory.parseString("x = 12"))

  println(cfg)

}
