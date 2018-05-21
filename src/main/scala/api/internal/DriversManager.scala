package api.internal

import java.io.File

import api.config.Preferences
import api.events.EventBus
import api.events.SensorsHubEvents._
import api.internal.MetadataFactory._
import api.internal.MetadataValidation._
import api.tasks.oph.TaskSchemaFactory
import org.apache.xbean.finder.ResourceFinder
import org.slf4j.{Logger, LoggerFactory}
import spi.drivers.Driver

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.runtime.universe._
import scala.util.Try

object DriversManager {
  org.apache.log4j.BasicConfigurator.configure() // dirty log4j conf for debug purpose TODO

  private[this] val driversDir = Preferences.cfg.driversDir
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)

  Option(new File(driversDir)).fold(logger.error(s"drivers directory ($driversDir) does not exists!")) {
      _.listFiles()
      .filter(_.getName.endsWith(".jar"))
      .map(_.toURI.toURL)
      .foreach(cl.addURL)
  }

  private val finder = new ResourceFinder("META-INF/", cl)
  private[this] implicit val logger: Logger = LoggerFactory.getLogger("sh.drivers-manager")

  private var driverPackages = Seq.empty[String]
  private val drivers: Map[String, (DriverMetadata, Class[Driver])] = detectAvailableDrivers()

  //@GrantWith(classOf[DriverManagementPermission], "drivers.list")
  def availableDrivers: Iterable[DriverMetadata] = {
    drivers.map(_._2._1)
  }

  val safeBoot: (DeviceDriver) => Try[Unit] = (drv: DeviceDriver) => Try {
    drv.controller.init()
    drv.controller.start()
  }

  def instanceDriver(name: String): Option[DeviceDriverWrapper] = {
    (for {
      driver <- drivers
      if driver._1 == name
      desc = driver._2._2.newInstance()
      ctrl <- compileDriverWithObservables(name, desc).toOption
      schemas = desc.tasks.map(cls => TaskSchemaFactory.createSchema(runtimeMirror(cl).classSymbol(cls).toType))
    } yield DeviceDriver(ctrl.configurator, ctrl, schemas, drivers(name)._1)).headOption
  }

  private def detectAvailableDrivers() : Map[String, (DriverMetadata, Class[Driver])] = {

    var names = Seq.empty[String]

    def checkNameConflict(name: String): Boolean = {
      if (names.contains(name)) {
        EventBus.trigger(DriverNameConflictWarn(name))
        false
      } else {
        names :+= name
        true
      }
    }

    val availableDrivers = for {
      props <- finder.mapAllProperties(classOf[Driver].getName).asScala
      metaCheck = validate(create(props._2))
      _ = metaCheck.fold(err => EventBus.trigger(DriverInvalidMetadataError(err)), _ => ())
      metadata <- metaCheck.toOption
      if checkNameConflict(metadata.name)
      cls <- tryDriverRegistration(metadata).toOption
    } yield metadata -> cls.asInstanceOf[Class[Driver]]

    availableDrivers map {x => x._1.name -> (x._1, x._2)} toMap

  }

  private def tryDriverRegistration(metadata: DriverMetadata): Try[Class[_]] = {
    val tryRegistration = Try {
      if(driverPackages.contains(metadata.descriptorClassName)) {
        throw new IllegalStateException(s"skipping ${metadata.name}: package clash for: ${metadata.descriptorClassName} (already loaded)")
      } else {
        driverPackages :+= metadata.descriptorClassName
        cl.loadClass(metadata.descriptorClassName)
      }
    }

    tryRegistration fold(
      err => EventBus.trigger(DriverLoadingError(err, metadata)),
      _ => EventBus.trigger(DriverLoaded(metadata)))

    tryRegistration
  }

  private def compileDriverWithObservables(name: String, desc: Driver): Try[DeviceController] = {
    val meta = drivers(name)._1
    val tryCompile = Try {
      Seq(desc.controllerClass, desc.configurationClass) foreach {
        cls =>
          if (driverPackages.contains(cls.getName) && !drivers.keys.toSeq.contains(name)) {
            //logger.error(s"class name clash: $cls")
            throw new IllegalStateException(s"conflict detected! class $cls already present.")
          } else {
            driverPackages :+= cls.getName
          }
      }

      val cfg = desc.configurationClass.getConstructors.head.newInstance(Seq(meta):_*).asInstanceOf[DeviceConfigurator]
      desc.controllerClass.getConstructors.head.newInstance(Seq(cfg):_*).asInstanceOf[DeviceController]
    }

    tryCompile fold(
        err => EventBus.trigger(DriverInstantiationError(err, meta)),
        ctrl => EventBus.trigger(DriverInstanced(meta)))
    
    tryCompile
  }
}