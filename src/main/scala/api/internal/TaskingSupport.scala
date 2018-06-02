package api.internal

import api.tasks.TaskSchema
import api.tasks.oph.TaskSchemaFactory
import fi.oph.myscalaschema.{ExtractionContext, SchemaFactory}
import io.reactivex.Maybe
import org.json4s.jackson.JsonMethods.parseOpt

import scala.collection.immutable.ListMap
import scala.reflect.runtime.universe._

trait TaskingSupport extends DeviceController {
  private[this] implicit val fmts = org.json4s.DefaultFormats

  @deprecated
  protected val commandClasses: List[Class[_]] = List.empty[Class[_]]

  /*private lazy val schemas = ListMap(commandClasses.map(cls => {
      val s = schemaFromClass(cls)
      val name = (s.toJson \\ "title").extract[String].replace(' ', '-')
      name -> s
    }):_*)*/

  private lazy val schemas = DriversManagerV2
    .tagFor(configurator.metadata.name)
    .map {
      tag =>
        ListMap(tag.descriptor.tasks.map(cls => {
          val s = schemaFromClass(cls, tag.classLoader)
          val name = (s.toJson \\ "title").extract[String].replace(' ', '-')
          name -> s
        }):_*)
    }.getOrElse(Map.empty[String,TaskSchema])

  println(schemas)

  protected val answer: PartialFunction[Any, Either[Option[String], Throwable]]

  private[this] implicit val context = ExtractionContext(SchemaFactory.default)

  private def schemaFromClass(cls: Class[_], cl: ClassLoader): TaskSchema =
    TaskSchemaFactory.createSchema(
      runtimeMirror(getClass.getClassLoader).classSymbol(cls).toType, cl)

  def send(task: String, msg: String): Maybe[String] = {
    val response = for {
      msg_ <- parseOpt(msg)
      schema <- schemas.get(task)
      result <- schema.extract(msg_).map(answer)
    } yield result

    Maybe.create[String](e => {
      val res = response match {
        case Some(x) => x
        case _ => Right(new IllegalArgumentException(
          s"unrecognized task or wrong message format: $task <- $msg"))
      }

      res match {
        case Right(reason) => e.onError(reason)
        case Left(Some(x)) => e.onSuccess(x)
        case Left(None) => e.onComplete()
      }
    })
  }
}