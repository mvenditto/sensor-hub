package driver_api

trait DeviceDriverWrapper {

  val config: DeviceConfiguration

  val controller: DeviceController with ObservablesSupport

}

case class DeviceDriver(
  config: DeviceConfiguration,
  controller: DeviceController with ObservablesSupport
) extends DeviceDriverWrapper
