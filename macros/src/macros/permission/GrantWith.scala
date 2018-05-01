package macros.permission

import java.security.BasicPermission

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@compileTimeOnly("enable macroparadise")
class GrantWith(permission: Class[_ <: BasicPermission], action: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro withPermission.impl
}
object withPermission {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    println(show(c.prefix.tree))

    val perm: Tree = c.prefix.tree match {
      case q"new WithPermission($permCls, $action)" =>
        (permCls collect {
          case q"classOf[$cls]" => q"new $cls($action)"
        }).head
      case _ => q""
    }

    println(s"permission: $perm")

    val result = annottees.map(_.tree).toList match {
      case q"$mods def $methodName[..$tpes](...$args): $returnType = { ..$body }" :: Nil =>
        q"""$mods def $methodName[..$tpes](...$args): $returnType = {
              securityManager.foreach(sm => sm.checkPermission($perm))
              ..$body
        }"""
      case _ => c.abort(c.enclosingPosition, "err")
    }

    c.Expr[Any](result)
  }
}