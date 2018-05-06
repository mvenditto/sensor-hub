package api.internal

import api.tasks.TaskSchema
import api.tasks.oph.TaskSchemaFactory
import fi.oph.myscalaschema.{ExtractionContext, SchemaFactory}
import io.reactivex.Maybe
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods.parseOpt

import scala.collection.immutable.ListMap
import scala.reflect.runtime.universe._

trait TaskingSupport {

  private[this] implicit val fmts = org.json4s.DefaultFormats

  protected val commandClasses: List[Class[_]] = List.empty[Class[_]]

  private lazy val schemas = ListMap(commandClasses.map(cls => {
      val s = schemaFromClass(cls)
      val name = (s.toJson \\ "title").extract[String].replace(' ', '-')
      println(name, s)
      name -> s
    }):_*)

  protected val answer: PartialFunction[Any, Either[Option[String], Throwable]]

  private[this] implicit val context = ExtractionContext(SchemaFactory.default)

  private def schemaFromClass(cls: Class[_]): TaskSchema =
    //SchemaFactory.default.createSchema(runtimeMirror(getClass.getClassLoader).classSymbol(cls).toType)
    TaskSchemaFactory.createSchema(runtimeMirror(getClass.getClassLoader).classSymbol(cls).toType)


  private def optExtractFromSchemas(json: JValue): Option[Any] = {
    for (schemaEntry <- schemas) {
      val(clsName, schema) = schemaEntry
      val result = schema.extract(json)
      //extractFrom(json, Class.forName(clsName, true, DriversManager.cl), schema).toOption
      if (result.isDefined) return result
    }
    None
  }

  def send(task: String, msg: String): Maybe[String] = {

    val response = for {
      msg_ <- parseOpt(msg)
      schema <- schemas.get(task)
      result <- schema.extract(msg_).map(answer)
    } yield result

    val res = response match {
      case Some(x) if x.isLeft && x.left.get.isDefined => x
      case Some(x) if x.isRight => x
      case _ => Right(new IllegalArgumentException(
        s"unrecognized task or wrong message format: $task <- $msg"))
    }

    Maybe.create[String](e => {
      res match {
        case Right(reason) => e.onError(reason)
        case Left(result) if result.isDefined => e.onSuccess(result.get)
        case Left(result) if result.isEmpty => e.onComplete()
      }
    })
  }
}