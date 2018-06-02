package api.tasks.oph

import api.tasks.{AbstractTaskSchemaFactory, TaskSchema}
import fi.oph.myscalaschema.{ExtractionContext, Schema, SchemaFactory}
import fi.oph.myscalaschema.SchemaValidatingExtractor.extractFrom
import fi.oph.myscalaschema.extraction.ObjectExtractor
import org.json4s.JsonAST.JValue

import scala.reflect.runtime.universe

class ScalaSchemaAdapter private[oph] (
  schema: Schema,
  className: String = "",
  classLoader: ClassLoader) extends TaskSchema {

  private[this] implicit val context: ExtractionContext = ExtractionContext(SchemaFactory.default)

  override def toJson: JValue = schema.toJson

  override def extract(json: JValue): Option[AnyRef] = synchronized {
    ObjectExtractor.overrideClassLoader(classLoader)
    val ext = extractFrom(json, Class.forName(className, true, classLoader), schema).toOption
    ObjectExtractor.overrideClassLoader(getClass.getClassLoader)
    ext
  }
}

object TaskSchemaFactory extends AbstractTaskSchemaFactory {

  override def createSchema[T](tpe: universe.Type, classLoader: ClassLoader): TaskSchema =
    new ScalaSchemaAdapter(
      SchemaFactory.default.createSchema(tpe),
      className = tpe.baseClasses.head.fullName,
      classLoader = classLoader
    )
}