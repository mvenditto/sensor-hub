package driver_api

import driver_api.internal.DriverManager
import fi.oph.myscalaschema.SchemaValidatingExtractor.extractFrom
import fi.oph.myscalaschema.{ExtractionContext, Schema, SchemaFactory}
import io.reactivex.Maybe
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods.parseOpt

import scala.collection.immutable.ListMap

trait MessageSupport {

  protected val schemas: ListMap[String, Schema] = ListMap.empty[String, Schema]
  protected val answer: PartialFunction[Any, Either[Option[String], Throwable]]

  private[this] implicit val context = ExtractionContext(SchemaFactory.default)

  private def optExtractFromSchemas(json: JValue): Option[Any] = {
    for (schemaEntry <- schemas) {
      val(clsName, schema) = schemaEntry
      val result = extractFrom(json, Class.forName(clsName, true, DriverManager.cl), schema).toOption
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