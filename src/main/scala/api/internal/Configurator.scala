package api.internal

import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

trait Configurator {

  protected implicit def camelCaseHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def configure(cfgPath: String): Unit

  def configureRaw(cfg: String): Unit

}

trait PersistedConfig extends Configurator {

  private[this] var cfg = Option.empty[Either[String, String]]

  def getConfig: Option[Either[String, String]] = cfg

  abstract override def configure(cfgPath: String): Unit = {
    cfg = Some(Left(cfgPath))
    super.configure(cfgPath)
  }

  abstract override def configureRaw(cfgRaw: String): Unit = {
    cfg = Some(Right(cfgRaw))
    super.configureRaw(cfgRaw)
  }
}