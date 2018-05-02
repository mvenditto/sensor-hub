package macros


import api.internal.Msg

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox


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

    val types = pack.typeSignature.members collect {
      case sym: ClassSymbol =>
        sym.baseClasses //somehow without this not annotations is detected, side effects??
        sym
    } withFilter {
      _.annotations.exists(_.tree.tpe <:< typeOf[Msg])
    } map { _.fullName }

    println("detected cmd types:", types)

    val result = annottees.map(_.tree).toList match {
      case q"$mods class $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" :: Nil =>
        val cmds = types.map(t => q"Class.forName($t)")
        q"""$mods class $tname extends { ..$earlydefns } with ..$parents {
          $self => ..$body
          override val tasks = List(..$cmds)
      }"""
      case _ => c.abort(c.enclosingPosition, "err")
    }

    c.Expr[Any](result)
  }
}
