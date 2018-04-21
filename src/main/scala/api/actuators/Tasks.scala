package api.actuators

import org.json4s.JValue

object Tasks {

  case class TaskingCapability(
    name: String,
    description: String,
    taskingParameters: JValue,
    properties: Option[JValue] = None
  )

}
