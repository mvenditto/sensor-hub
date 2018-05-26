package api.devices

import java.net.URI

import api.tasks.Tasks.TaskingCapability
import api.internal.DeviceDriverWrapper
import api.sensors.Sensors.{DataStream, DataStreamCustomProps, Encoding}
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
    customProps: Map[String, DataStreamCustomProps] = Map.empty
  ) {
    val dataStreams: Iterable[DataStream] =
      driver.controller.dataStreams.map(ds => {
        var updatedDs = ds.copy(sensor = this)
        customProps.get(ds.name).foreach(cp => {
          updatedDs = updatedDs.copy(
            name = cp.name.getOrElse(updatedDs.name),
            description = cp.description.getOrElse(updatedDs.description),
            featureOfInterest = cp.featureOfInterest.getOrElse(updatedDs.featureOfInterest),
          )
        })
        updatedDs
      })

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
