package utils

object SecurityUtils {

  lazy val securityManager = Option(System.getSecurityManager)

}
