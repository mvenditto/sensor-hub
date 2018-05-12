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
        logger.info(s"[${evt.timestamp}] device created: ${metadata.name}[${metadata.id}]")

      case DriverNameConflictWarn(name) =>
        logger.warn(s"[${evt.timestamp}] skipping $name: name conflicting.")

      case DriverLoaded(metadata) =>
        logger.info(s"[${evt.timestamp}] loaded driver: ${metadata.name}")

      case DriverLoadingError(err, metadata) =>
        logger.error(s"[${evt.timestamp}] error loading driver ${metadata.name}, cause: ${err.getMessage}")

      case DriverInvalidMetadataError(err) =>
        logger.error(s"[${evt.timestamp}] invalid metadata: ${err.msg}")

      case DriverInstanced(metadata) =>
        logger.info(s"[${evt.timestamp}] instanced driver: ${metadata.name}")

      case DriverInstantiationError(err, metadata) =>
        logger.error(s"[${evt.timestamp}] error instancing driver ${metadata.name}, cause: ${err.getMessage}")

      case ServiceLoaded(metadata) =>
        logger.info(s"[${evt.timestamp}] service loaded: ${metadata.name}")

      case ServiceLoadingError(err, metadata) =>
        logger.info(s"[${evt.timestamp}] error loading service ${metadata.name}, cause: ${err.getMessage}")

      case e =>
        logger.info(s"[${e.timestamp}] unknown event: $e")
    })
  }

}
