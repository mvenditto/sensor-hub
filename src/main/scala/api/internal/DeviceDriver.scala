package api.internal

import api.tasks.TaskSchema

trait DeviceDriverWrapper {
  val config: DeviceConfigurator
  val controller: DeviceController
  val tasks: Iterable[TaskSchema]
  val metadata: DriverMetadata
}

case class DeviceDriver(
  config: DeviceConfigurator,
  controller: DeviceController,
  tasks: Iterable[TaskSchema],
  metadata: DriverMetadata
) extends DeviceDriverWrapper
