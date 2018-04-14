package driver_api

import pureconfig._
import pureconfig.{ProductHint, CamelCase, ConfigFieldMapping}

trait DeviceConfigurator {

  protected implicit def camelCaseHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  private var jniLibPath: Option[String] = Option.empty[String]

  def configure(cfgPath: String): Unit

  def getJniLibPath: Option[String] = jniLibPath

  def setJniLibPath(jniLibs: String): Unit = { jniLibPath = Some(jniLibs) }

}
