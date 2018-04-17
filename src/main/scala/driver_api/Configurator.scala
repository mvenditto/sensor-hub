package driver_api

import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

trait Configurator {

  protected implicit def camelCaseHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def configure(cfgPath: String): Unit

}
