package api.internal

import fi.oph.myscalaschema.SchemaValidatingExtractor.extractFrom
import fi.oph.myscalaschema.{ExtractionContext, Schema, SchemaFactory}
import io.reactivex.Maybe
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods.parseOpt

import scala.collection.immutable.ListMap
import scala.reflect.runtime.universe._

trait TaskingSupport {

  protected val commandClasses: List[Class[_]] = List.empty[Class[_]]
  private lazy val schemas = ListMap(commandClasses.map(cls => (cls.getName, schemaFromClass(cls))):_*)
  protected val answer: PartialFunction[Any, Either[Option[String], Throwable]]

  private[this] implicit val context = ExtractionContext(SchemaFactory.default)

  private def schemaFromClass(cls: Class[_]): Schema =
    SchemaFactory.default.createSchema(runtimeMirror(getClass.getClassLoader).classSymbol(cls).toType)

  private def optExtractFromSchemas(json: JValue): Option[Any] = {
    for (schemaEntry <- schemas) {
      val(clsName, schema) = schemaEntry
      val result = extractFrom(json, Class.forName(clsName, true, DriversManager.cl), schema).toOption
      if (result.isDefined) return result
    }
    None
  }

  def send(msg: String): Maybe[String] = {
    val response = for {
      result <- parseOpt(msg)
      msg <- optExtractFromSchemas(result).map(answer)
    } yield msg

    val res = response match {
      case Some(x) if x.isLeft && x.left.get.isDefined => x
      case Some(x) if x.isRight => x
      case _ => Left(None)
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