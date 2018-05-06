package api.internal

import java.io.File
import java.net.URI

import api.sensors.DevicesManager
import api.internal.MetadataFactory._
import api.internal.MetadataValidation._
import api.sensors.Sensors.Encodings
import api.services.security.permission.DriverManagementPermission
import api.tasks.oph.TaskSchemaFactory
import spi.drivers.Driver
import fi.oph.myscalaschema.extraction.ObjectExtractor
import macros.permission.GrantWith
import org.apache.xbean.finder.ResourceFinder
import org.slf4j.{Logger, LoggerFactory}
import utils.LoggingUtils.{logEitherOpt, logTry}

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.util.Try
import scala.reflect.runtime.universe._
import utils.SecurityUtils.securityManager

object DriversManager {
  org.apache.log4j.BasicConfigurator.configure() // dirty log4j conf for debug purpose TODO

  var driversDir = "../ext/drivers/"
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)

  new File(driversDir)
    .listFiles()
    .filter(_.getName.endsWith(".jar"))
    .map(_.toURI.toURL)
    .foreach(cl.addURL)

  private val finder = new ResourceFinder("META-INF/", cl)
  private[this] implicit val logger: Logger = LoggerFactory.getLogger("sh.drivers-manager")

  private var driverPackages = Seq.empty[String]
  private val drivers: Map[String, (DriverMetadata, Class[Driver])] = detectAvailableDrivers()

  //@GrantWith(classOf[DriverManagementPermission], "drivers.list")
  def availableDrivers: Iterable[DriverMetadata] = {
    drivers.map(_._2._1)
  }

  def instanceDriver(name: String): Option[DeviceDriverWrapper] = {
    (for {
      driver <- drivers
      if driver._1 == name
      desc = driver._2._2.newInstance()
      ctrl <- compileDriverWithObservables(name, desc, "").toOption
      schemas = desc.tasks.map(cls => TaskSchemaFactory.createSchema(runtimeMirror(cl).classSymbol(cls).toType))
    } yield DeviceDriver(ctrl.configurator, ctrl, schemas, drivers(name)._1)).headOption
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
        cl.loadClass(metadata.descriptorClassName)
      }
    }

    logTry(tryRegistration)(
      err => s"""driver loading error ${metadata.name}: ${err.getMessage}""",
      cls => s"""loaded driver descriptor: ${metadata.name} :$cls"""
    )

    tryRegistration
  }

  private def compileDriverWithObservables(name: String, desc: Driver, nativeLibsPath: String): Try[DeviceController] = {
    val tryCompile = Try {
      Seq(desc.controllerClass, desc.configurationClass) foreach {
        cls =>
          if (driverPackages.contains(cls.getName) && !drivers.keys.toSeq.contains(name)) {
            logger.error(s"class name clash: $cls")
            throw new IllegalStateException(s"conflict detected! class $cls already present.")
          } else {
            driverPackages :+= cls.getName
          }
      }

      val cfg = desc.configurationClass.newInstance()
      cfg.setJniLibPath(nativeLibsPath)
      desc.controllerClass.getConstructors.head.newInstance(Seq(cfg):_*).asInstanceOf[DeviceController]
    }

    logTry(tryCompile)(
      err => s"instantiation error: ${err.getMessage}",
      _ => s"instantiated driver for: ${desc.controllerClass}:${desc.configurationClass}"
    )
    tryCompile
  }
}

object TestServices extends App  {
  ObjectExtractor.overrideClassLoader(DriversManager.cl)

  val d1 = DriversManager.instanceDriver("driver 1")

  println(DriversManager.availableDrivers)

  import org.json4s.jackson.JsonMethods._

  d1.foreach {
    drv =>
      drv.controller.init()
      drv.controller.start()
      println("sensor1")
      val s1 = DevicesManager.createDevice("test temp sensor", "", Encodings.PDF, new URI(""), drv)
  }

  val d2 = DriversManager.instanceDriver("driver 1")

  d2.foreach {
    drv =>
      drv.controller.init()
      drv.controller.start()
      println("sensor2")
      val s1 = DevicesManager.createDevice("test temp sensor2", "", Encodings.PDF, new URI(""), drv)
      s1.tasks.foreach(t => println(t))
  }

  //DevicesManager.obsBus.subscribe(println(_))


  //println(DevicesManager.sensors)

}
