
import java.io.{File, FileWriter}
import java.net.URI

import api.config.Preferences
import api.config.Preferences.configure
import api.devices.Devices.Device
import api.devices.DevicesManager
import api.devices.Sensors.Encodings
import api.events.EventLogging
import api.internal.{DeviceConfigurator, DisposableManager, DriversManager, PersistedConfig}
import api.services.ServicesManager
import org.apache.commons.daemon.{Daemon, DaemonContext}
import org.apache.log4j.{Level, LogManager}
import org.json4s.JsonAST.{JField, JInt, JObject, JString}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods.parseOpt
import utils.CustomSeriDeseri
import utils.CustomSeriDeseri.DeviceMetadataWithId

import scala.io.Source

class BootDaemon extends Daemon {

  private[this] val logger = LoggerFactory.getLogger("sh.daemon")
  private[this] implicit val formats = CustomSeriDeseri.fmt

  val tag =
    """
       ::: version 0.4.3a :::                         :::           :::
        __   ___  _ __   ___   ___   _ __  ___        | |__   _   _ | |__
      / __| / _ \| '_ \ / __| / _ \ |  __|/ __| _____ |  _ \ | | | ||  _ \
      \__ \|  __/| | | |\__ \| (_) || |   \__ \|_____|| | | || |_| || |_) |
   :::|___/ \___||_| |_||___/ \___/ |_|   |___/       |_| |_| \____||____/.:::::

    """

  private[this] var badShutdown: Boolean = false

  override def init(context: DaemonContext): Unit = {
      configure("sh-prefs.conf")
      if (Preferences.cfg.logEvents) EventLogging.init()
      org.apache.log4j.BasicConfigurator.configure()
      System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog")
      System.setProperty("org.eclipse.jetty.LEVEL", "OFF")
      logger.info("attaching graceful shutdown jvm hook...")
      Runtime.getRuntime.addShutdownHook(new Thread(() => shutdownGracefully()))
  }

  override def start(): Unit = {
    Try {
      val snapshot = new File(".snapshot")
      if (snapshot.exists()) {
        parseOpt(Source.fromFile(snapshot).mkString)
          .map(_.extract[List[DeviceMetadataWithId]])
          .foreach(devices => devices.foreach(tryRestoreDevice))
      }

      Await.ready(ServicesManager.runAllServices(), 5 seconds)
      import scala.collection.JavaConverters._
      LogManager.getCurrentLoggers.asScala foreach {
        case l: org.apache.log4j.Logger =>
          if (!l.getName.startsWith("sh.")) l.setLevel(Level.OFF)
      }
      logger.info(tag)
    } recover {
      case err =>
        badShutdown = true
        throw err
    }
  }

  override def destroy(): Unit = {
    logger.debug("destroy")
  }

  override def stop(): Unit = {
    logger.debug("stop")
  }

  def tryRestoreDevice(dev: DeviceMetadataWithId): Option[Device] = {
    logger.info(s"attempt to restore device: ${dev.name}[cfg:${dev.cfg.nonEmpty}]")
    DriversManager.instanceDriver(dev.driverName).map {
      drv =>
        if (dev.cfg.nonEmpty) {
          if (dev.cfg.startsWith("raw:")) drv.config.configureRaw(dev.cfg.split("raw:").tail.mkString)
          else drv.config.configure(dev.cfg)
        }
        drv.controller.init()
        drv.controller.start()
        DevicesManager.createDevice(dev.name, dev.description,
          Encodings.fromName(dev.metadataEncoding), new URI(dev.metadata), drv)
    }
  }

  def snapshotSystem(): Try[Unit] = Try {

    val devices = write(DevicesManager.devices().map(dev => {

      val cfg = dev.driver.config match {
        case c: DeviceConfigurator with PersistedConfig =>
          c.getConfig.map(_.fold(s => s, r => "raw:"+r))
        case _ => None
      }

      JObject(
            JField("id", JInt(dev.id)) ::
            JField("name", JString(dev.name)) ::
            JField("description", JString(dev.description)) ::
            JField("metadataEncoding", JString(dev.encodingType.name)) ::
            JField("metadata", JString(dev.metadata.toString)) ::
            JField("driverName", JString(dev.driver.metadata.name)) ::
            JField("cfg", JString(cfg.getOrElse("")))
              :: Nil)
      }
    ))

    val snapshot = new File(".snapshot")
    if (snapshot.exists()) snapshot.delete()
    snapshot.createNewFile()
    val fw = new FileWriter(snapshot)
    fw.write(devices)
    fw.close()
  }

  def shutdownGracefully(): Unit = {

    logger.info("starting graceful shutdown")

    logger.info("saving system state...")
    snapshotSystem()

    logger.info("stopping all services...")

    Await.ready(
      Future.sequence(ServicesManager.registeredServices
        .map(_.name)
        .map(ServicesManager.shutdownService)), 10 seconds)
      .onComplete(_.fold(
        err => logger.error(s"error while shutting down services: $err"),
        _ => ()
      ))

    logger.info("disposing resources...")
    DisposableManager.disposeAll()

    logger.info("bye")
  }

}

object Boot extends App {

  val daemon = new BootDaemon()
  daemon.init(null)
  daemon.start()

}
