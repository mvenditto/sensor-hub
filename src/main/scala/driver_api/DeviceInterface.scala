package driver_api

import fi.oph.myscalaschema.Schema

import scala.annotation.StaticAnnotation
import scala.collection.immutable.ListMap

case class Msg() extends StaticAnnotation

trait DeviceInterface {
  val schemas: ListMap[String, Schema] = ListMap()
}



