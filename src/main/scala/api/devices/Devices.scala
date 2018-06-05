package api.devices

import java.net.URI

import api.tasks.Tasks.TaskingCapability
import api.internal.DeviceDriverWrapper
import Sensors.{DataStream, DataStreamCustomProps, Encoding}


object Devices {

  sealed trait DeviceType

  case object Sensor extends DeviceType
  case object Actuator extends DeviceType
  case object SensorAndActuator extends DeviceType

  class Device private[devices] (
    val id: Int,
    val name: String,
    val description: String,
    val encodingType: Encoding,
    val metadata: URI,
    val driver: DeviceDriverWrapper,
    val tasks: Iterable[TaskingCapability],
    _dataStreams: Iterable[DataStream],
    val customProps: Map[String, DataStreamCustomProps] = Map.empty
  ) {
    val dataStreams: Iterable[DataStream] = _dataStreams.map(ds => ds.updateWith(newSensor = this))
  }

  object Device {
    def apply(
      id: Int, name: String, description: String,
      encodingType: Encoding, metadata: URI,
      driver: DeviceDriverWrapper,
      customProps: Map[String, DataStreamCustomProps] = Map.empty): Device = {
      new Device(id, name, description, encodingType, metadata,
        driver, mapTasks(driver), mapDataStreams(customProps, driver), customProps)
    }

    private[devices] def mapTasks(driver: DeviceDriverWrapper): Iterable[TaskingCapability] = {
      driver.tasks
        .map(_.toJson)
        .map(s => TaskingCapability(
          name = s \\ "id" toString,
          description = s \\ "description" toString,
          taskingParameters = s
        ))
    }

    private[devices] def mapDataStreams(customProps: Map[String, DataStreamCustomProps], driver: DeviceDriverWrapper): Iterable[DataStream] = {
      driver.controller.dataStreams.map(ds => {
        var updatedDs = ds
        customProps.get(ds.name).foreach(cp => {
          updatedDs = updatedDs.updateWith(
            newName = cp.name.getOrElse(updatedDs.name),
            newDescription = cp.description.getOrElse(updatedDs.description),
            newFeatureOfInterest = cp.featureOfInterest.getOrElse(updatedDs.featureOfInterest)
          )
        })
        updatedDs
      })
    }
  }
}
