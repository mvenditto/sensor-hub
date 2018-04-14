package driver_api

trait DeviceDriverWrapper {

  val config: DeviceConfigurator

  val controller: DeviceController with ObservablesSupport

}

case class DeviceDriver(
  config: DeviceConfigurator,
  controller: DeviceController with ObservablesSupport
) extends DeviceDriverWrapper
