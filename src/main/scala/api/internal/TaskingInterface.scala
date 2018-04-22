package api.internal

import api.tasks.TaskSchema

import scala.annotation.StaticAnnotation
import scala.collection.immutable.ListMap

case class Msg() extends StaticAnnotation

trait TaskingInterface {

  val supportedTasks: ListMap[String, TaskSchema] = ListMap()

}



