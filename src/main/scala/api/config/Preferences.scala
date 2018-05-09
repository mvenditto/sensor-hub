package api.config

import java.io.File

import api.internal.Configurator
import pureconfig.loadConfigFromFiles

object Preferences extends Configurator {

  case class ShConfig(
    driversDir: String = "../ext/drivers/",
    servicesDir: String = "../ext/services/",
    logEvents: Boolean = true
  )

  configure("sh-prefs.conf")

  private var _cfg: ShConfig = _

  lazy val cfg: ShConfig = _cfg

  override def configure(cfgPath: String): Unit = {
    _cfg = loadConfigFromFiles[ShConfig](Seq(cfgPath).map(new File(_).toPath)) match {
      case Right(config) =>
        println(config)
        config
      case Left(e) =>
        println("error", e)
        ShConfig()
    }
  }
}
