package driver_api

trait DeviceDriverWrapper {

  val config: DeviceConfigurator

  val controller: DeviceController

}

case class DeviceDriver(
  config: DeviceConfigurator,
  controller: DeviceController
) extends DeviceDriverWrapper
