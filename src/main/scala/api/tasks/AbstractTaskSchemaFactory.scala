package api.tasks

import reflect.runtime.universe.Type

trait AbstractTaskSchemaFactory {

  def createSchema[T](tpe: Type): TaskSchema

}

