package api.events

import api.events.SensorsHubEvents._
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory

object EventLogging {

  private[this] val logger = LoggerFactory.getLogger("sh.event-bus")

  def init(): Unit = {
    EventBus.events
      .subscribeOn(Schedulers.io())
      .subscribe(evt => evt match {

      case DeviceCreated(metadata) =>
        logger.info(s"device created: ${metadata.name}[${metadata.id}]")

      case DeviceDeleted(metadata) =>
        logger.info(s"device deleted: ${metadata.name}[${metadata.id}]")

      case DriverNameConflictWarn(name) =>
        logger.warn(s"skipping $name: name conflicting.")

      case DriverLoaded(metadata) =>
        logger.info(s"loaded driver: ${metadata.name}")

      case DriverLoadingError(err, metadata) =>
        logger.error(s"error loading driver ${metadata.name}, cause: ${err.getMessage}")

      case DriverInvalidMetadataError(err) =>
        logger.error(s"invalid metadata: ${err.msg}")

      case DriverInstanced(metadata) =>
        logger.info(s"instanced driver: ${metadata.name}[${metadata.rootDir}]")

      case DriverInstantiationError(err, metadata) =>
        logger.error(s"error instancing driver ${metadata.name}, cause: ${err.getMessage}")

      case ServiceLoaded(metadata) =>
        logger.info(s"service loaded: ${metadata.name}")

      case ServiceLoadingError(err, metadata) =>
        logger.info(s"error loading service ${metadata.name}, cause: ${err.getMessage}")

      case e =>
        logger.info(s"unknown event: $e")
    })
  }

}
