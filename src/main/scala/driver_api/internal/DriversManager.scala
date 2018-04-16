package driver_api.internal

import java.io.File

import driver_api._
import driver_api.internal.MetadataFactory._
import driver_api.internal.MetadataValidation._
import driver_api.spi.Driver
import fi.oph.myscalaschema.extraction.ObjectExtractor
import org.apache.xbean.finder.ResourceFinder
import org.slf4j.{Logger, LoggerFactory}
import utils.LoggingUtils.{logEitherOpt, logTry}

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.tools.reflect.ToolBox
import scala.util.Try

object DriversManager {
  org.apache.log4j.BasicConfigurator.configure() // dirty log4j conf for debug purpose TODO

  var driversDir = "/home/pps/electrom/scalajs-electron-skeleton/verlet/verlet/sensors-hub/out/artifacts/ext/"
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)

  new File(driversDir)
    .listFiles()
    .filter(_.getName.endsWith(".jar"))
    .map(_.toURI.toURL)
    .foreach(cl.addURL)

  private val finder = new ResourceFinder("META-INF/", cl)
  private[this] implicit val logger: Logger = LoggerFactory.getLogger("drivers-manager")

  private var driverPackages = Seq.empty[String]
  private val drivers: Map[String, (DriverMetadata, Class[Driver])] = detectAvailableDrivers()

  def availableDrivers: Iterable[DriverMetadata] = drivers.map(_._2._1)

  def instanceDriver(name: String): Option[DeviceDriverWrapper] = {
    (for {
      driver <- drivers
      if driver._1 == name
      desc = driver._2._2.newInstance()
      ctrl <- compileDriverWithObservables(
        desc.controllerClass, desc.configurationClass, "").toOption
    } yield DeviceDriver(ctrl.configurator, ctrl)).headOption
  }

  private def detectAvailableDrivers() : Map[String, (DriverMetadata, Class[Driver])] = {

    var names = Seq.empty[String]

    def checkNameConflict(name: String): Boolean = {
      if (names.contains(name)) {
        logger.warn(s"skipping $name: name conflicting.")
        false
      } else {
        names :+= name
        true
      }
    }

    val availableDrivers = for {
      props <- finder.mapAllProperties(classOf[Driver].getName).asScala
      metadata <- logEitherOpt(validate(create(props._2)))
      if checkNameConflict(metadata.name)
      cls <- tryDriverRegistration(metadata).toOption
    } yield metadata -> cls.asInstanceOf[Class[Driver]]

    availableDrivers map {x => x._1.name -> (x._1, x._2)} toMap

  }

  private def tryDriverRegistration(metadata: DriverMetadata): Try[Class[_]] = {
    val tryRegistration = Try {
      if(driverPackages.contains(metadata.descriptorClassName)) {
        logger.error(s"skipping ${metadata.name}: package clash for: ${metadata.descriptorClassName} (already loaded)")
        throw new IllegalStateException()
      } else {
        driverPackages :+= metadata.descriptorClassName
        cl.loadClass( metadata.descriptorClassName)
      }
    }

    logTry(tryRegistration)(
      err => s"""driver loading error ${metadata.name}: ${err.getMessage}""",
      cls => s"""loaded driver descriptor: ${metadata.name} :$cls"""
    )

    tryRegistration
  }

  private def compileDriverWithObservables(
    ctrlClass: Class[_], configClass: Class[_], nativeLibsPath: String): Try[DeviceController] = {
    val tryCompile = Try {
      Seq(ctrlClass, configClass) foreach {
        cls =>
          if (driverPackages.contains(cls.getName)) {
            logger.error(s"class name clash: $cls")
            throw new IllegalStateException(s"conflict detected! class $cls already present.")
          } else {
            driverPackages :+= cls.getName
          }
      }
      val cfg = configClass.newInstance().asInstanceOf[DeviceConfigurator]
      cfg.setJniLibPath(nativeLibsPath)
      ctrlClass.getConstructors.head.newInstance(Seq(cfg):_*).asInstanceOf[DeviceController]
    }

    logTry(tryCompile)(
      err => s"instantiation error: ${err.getMessage}",
      _ => s"instantiated driver for: $ctrlClass:$configClass"
    )
    tryCompile
  }
}

object TestServices extends App  {
  ObjectExtractor.overrideClassLoader(DriversManager.cl)

  val d1 = DriversManager.instanceDriver("driver 1")

  println(DriversManager.availableDrivers)

  d1.foreach {
    drv =>
      drv.controller.init()
      drv.controller.start()
      val ds1 = drv.controller.dataStreams.head
      ds1.observable.subscribe(obs => println(obs))
      //ctrl.send("""{"message":"Ciao"}""")
  }

}
