package api.events

import api.devices.Devices.Device
import api.internal.DriverMetadata
import api.internal.MetadataValidation.ValidationError
import spi.service.ServiceMetadata

object SensorsHubEvents {

  sealed trait SensorsHubEvent { val timestamp: Long = System.currentTimeMillis() }

  class Error extends SensorsHubEvent
  class Warn extends SensorsHubEvent
  class Info extends SensorsHubEvent

  case class DeviceCreated(ds: Device) extends Info

  case class DeviceDeleted(ds: Device) extends Info

  case class DriverNameConflictWarn(name: String) extends Warn

  case class DriverNameClashError(metadata: DriverMetadata) extends Error

  case class DriverLoaded(metadata: DriverMetadata) extends Info

  case class DriverLoadingError(t: Throwable, metadata: DriverMetadata) extends Error

  case class DriverInvalidMetadataError(err: ValidationError) extends Error

  case class DriverInstanced(metadata: DriverMetadata) extends Info

  case class DriverInstantiationError(t: Throwable, metadata: DriverMetadata) extends Error

  case class ServiceLoaded(metadata: ServiceMetadata) extends Info

  case class ServiceLoadingError(t: Throwable, metadata: ServiceMetadata) extends Error

}
