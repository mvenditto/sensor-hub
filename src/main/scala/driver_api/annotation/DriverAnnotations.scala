package driver_api.annotation

import scala.annotation.StaticAnnotation

object DriverAnnotations {

  case class Metadata(
    description: String,
    configClass: String,
    controllerClass: String,
    nativeLibs: List[String] = List()
  ) extends StaticAnnotation

  trait WithCustomJsonFormat

}