package driver_api

trait DeviceDriverWrapper {

  val config: DeviceConfiguration

  val controller: DeviceController

}

case class DeviceDriver(
  config: DeviceConfiguration,
  controller: DeviceController
) extends DeviceDriverWrapper
