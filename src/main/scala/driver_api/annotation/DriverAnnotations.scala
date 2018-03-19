package driver_api.annotation

import scala.annotation.StaticAnnotation

object DriverAnnotations {

  case class Metadata(
    description: String,
    configClass: String,
    controllerClass: String
  ) extends StaticAnnotation

  trait WithCustomJsonFormat

}