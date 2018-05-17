package api.devices

import java.net.URI

import api.tasks.Tasks.TaskingCapability
import api.internal.DeviceDriverWrapper
import api.sensors.Sensors.{DataStream, Encoding}
import org.json4s.JsonDSL._


object Devices {

  sealed trait DeviceType

  case object Sensor extends DeviceType
  case object Actuator extends DeviceType
  case object SensorAndActuator extends DeviceType

  case class Device private (
    id: Int, name: String, description: String,
    encodingType: Encoding, metadata: URI,
    driver: DeviceDriverWrapper,
    dataStreamMapper:(DataStream) => DataStream = ds => ds
  ) {
    val dataStreams: Iterable[DataStream] =
      driver.controller.dataStreams.map(ds => dataStreamMapper(ds.copy(sensor = this)))
    val tasks: Iterable[TaskingCapability] =
      driver.tasks
        .map(_.toJson)
        .map(s => TaskingCapability(
          name = s \\ "id" toString,
          description = s \\ "description" toString,
          taskingParameters = s
        ))
  }

}
