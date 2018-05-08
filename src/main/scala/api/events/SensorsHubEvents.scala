package api.events

import api.devices.Devices.Device
import api.internal.MetadataValidation.ValidationError
import api.internal.{DeviceController, DriverMetadata}
import spi.service.ServiceMetadata

object SensorsHubEvents {

  sealed trait SensorsHubEvent

  case class DeviceCreated(ds: Device) extends SensorsHubEvent

  case class DeviceDeleted(ds: Device) extends SensorsHubEvent

  case class DriverNameConflictWarn(name: String) extends SensorsHubEvent

  case class DriverNameClashError(metadata: DriverMetadata) extends SensorsHubEvent

  case class DriverLoaded(clazz: Class[_], metadata: DriverMetadata) extends SensorsHubEvent

  case class DriverLoadingError(t: Throwable, metadata: DriverMetadata) extends SensorsHubEvent

  case class DriverInvalidMetadataError(err: ValidationError) extends SensorsHubEvent

  case class DriverInstanced(ctrl: DeviceController, metadata: DriverMetadata) extends SensorsHubEvent

  case class DriverInstantiationError(t: Throwable, metadata: DriverMetadata) extends SensorsHubEvent

  case class ServiceLoaded(metadata: ServiceMetadata) extends SensorsHubEvent

  case class ServiceLoadingError(t: Throwable, metadata: ServiceMetadata) extends SensorsHubEvent

}
