package api.tasks.oph

import api.internal.DriversManager
import api.tasks.{AbstractTaskSchemaFactory, TaskSchema}
import fi.oph.myscalaschema.{ExtractionContext, Schema, SchemaFactory}
import fi.oph.myscalaschema.SchemaValidatingExtractor.extractFrom
import fi.oph.myscalaschema.extraction.ObjectExtractor
import org.json4s.JsonAST.JValue

import scala.reflect.runtime.universe

class ScalaSchemaAdapter private[oph] (schema: Schema, className: String = "") extends TaskSchema {

  private[this] implicit val context: ExtractionContext = ExtractionContext(SchemaFactory.default)

  override def toJson: JValue = schema.toJson

  override def extract(json: JValue): Option[AnyRef] =
    extractFrom(json, Class.forName(className, true, DriversManager.cl), schema).toOption
}

object TaskSchemaFactory extends AbstractTaskSchemaFactory {

  ObjectExtractor.overrideClassLoader(DriversManager.cl)

  override def createSchema[T](tpe: universe.Type): TaskSchema =
    new ScalaSchemaAdapter(
      SchemaFactory.default.createSchema(tpe),
      className = tpe.baseClasses.head.fullName
    )
}