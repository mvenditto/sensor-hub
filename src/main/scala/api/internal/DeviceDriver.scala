package api.internal

import fi.oph.myscalaschema.Schema

trait DeviceDriverWrapper {
  val config: DeviceConfigurator
  val controller: DeviceController
  val tasks: Iterable[Schema]
}

case class DeviceDriver(
  config: DeviceConfigurator,
  controller: DeviceController,
  tasks: Iterable[Schema]
) extends DeviceDriverWrapper
