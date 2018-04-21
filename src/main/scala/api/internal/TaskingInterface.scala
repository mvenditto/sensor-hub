package api.internal

import fi.oph.myscalaschema.Schema

import scala.annotation.StaticAnnotation
import scala.collection.immutable.ListMap

case class Msg() extends StaticAnnotation

trait TaskingInterface {

  val supportedTasks: ListMap[String, Schema] = ListMap()

}



