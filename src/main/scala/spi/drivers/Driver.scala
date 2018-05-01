package spi.drivers

import api.internal.{DeviceConfigurator, DeviceController}

trait Driver {

  val controllerClass: Class[_ <: DeviceController]

  val configurationClass: Class[_ <: DeviceConfigurator]

  val tasks: List[Class[_]]

}
