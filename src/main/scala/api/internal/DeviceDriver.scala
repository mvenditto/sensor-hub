package api.internal

import api.tasks.TaskSchema

trait DeviceDriverWrapper {
  val config: DeviceConfigurator
  val controller: DeviceController
  val tasks: Iterable[TaskSchema]
}

case class DeviceDriver(
  config: DeviceConfigurator,
  controller: DeviceController,
  tasks: Iterable[TaskSchema]
) extends DeviceDriverWrapper
