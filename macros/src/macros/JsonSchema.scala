package macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

case class CustomJsonFormat() extends StaticAnnotation

@compileTimeOnly("enable macroparadise")
class CollectSchemasFrom(packageName: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro inputInterface.impl
}
object inputInterface {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val pack = c.prefix.tree match {
      case q"new CollectSchemasFrom($pkg)" =>
        c.mirror.staticPackage(pkg.collect {
          case Literal(Constant(name: String)) => name
        }.head)
    }

    val hasUserDefinedFormat = (s: ClassSymbol) =>
      s.baseClasses.map(_.fullName).exists(_.split('.').last == "WithCustomJsonFormat")

    val types = pack.typeSignature.members collect {
      case sym: ClassSymbol => sym
    } filterNot { hasUserDefinedFormat(_) } map {
      sym =>
        val name = sym.fullName
        val numCaseAcc = sym.asClass.selfType.members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m
        }.size
        name -> numCaseAcc
    }

    val msgNames = types.map(cls => q"""${cls._1} ->
      SchemaFactory.default.createSchema(runtimeMirror(getClass.getClassLoader).classSymbol(Class.forName(${cls._1})).toType)""").toList

    val result = annottees.map(_.tree).toList match {
      case q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" :: Nil =>
        q"""$mods object $tname extends { ..$earlydefns } with ..$parents {
          $self => ..$body
          import fi.oph.myscalaschema.SchemaFactory
          import scala.collection.immutable.ListMap
          import scala.reflect.runtime.universe._
          override val schemas = ListMap(..$msgNames)
      }"""
      case _ => c.abort(c.enclosingPosition, "err")
    }

    c.Expr[Any](result)
  }
}
