package api.events

import api.devices.Devices.Device

object SensorsHubEvents {

  sealed trait SensorsHubEvent

  case class DeviceCreated(ds: Device) extends SensorsHubEvent

  case class DeviceDeleted(ds: Device) extends SensorsHubEvent

}
