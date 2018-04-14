package driver_api.spi

import driver_api.{DeviceConfigurator, DeviceController}

trait Driver {

  val controllerClass: Class[_ <: DeviceController]

  val configurationClass: Class[_ <: DeviceConfigurator]

  val schemas: List[Class[_]]

}
