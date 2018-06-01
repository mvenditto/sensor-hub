package api.internal

import java.io.File
import java.net.URL
import java.util.Properties

import api.config.Preferences
import api.events.EventBus
import api.events.SensorsHubEvents._
import api.internal.MetadataFactory._
import api.internal.MetadataValidation._
import api.tasks.oph.TaskSchemaFactory
import spi.drivers.Driver

import fi.oph.myscalaschema.extraction.ObjectExtractor
import org.apache.xbean.finder.ResourceFinder

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.reflect.runtime.universe._
import scala.tools.reflect._
import scala.util.Try

object testV2 extends App {

  import DriversManagerV2.initAndStart

  Preferences.configure("sh-prefs.conf")

  DriversManagerV2
    .instanceDriver("therm1")
    .map(initAndStart)
    .map(_.controller.dataStreams.head.doObservation().result)
    .foreach(println)
}

object DriversManagerV2{

  case class DriverTag(
    id: String,
    metadata: DriverMetadata,
    descriptor: Driver,
    classLoader: ClassLoader
  )

  private[this] val jars = Option(new File(Preferences.cfg.driversDir))
    .map(_.listFiles().filter(_.getName.endsWith(".jar")).map(_.toURI.toURL))

  private[this] val uri = classOf[Driver].getName
  private[this] val finder = new ResourceFinder("META-INF/", jars.getOrElse(Array.empty[URL]):_*)

  private[this] val driverTags = for {
    props <- finder.mapAllProperties(uri).asScala
    url <- finder.getResourcesMap(uri).asScala
    metadata <- extractMetadata(props._2)
    id = props._1
    jarURL = new URL(url._2.getFile.split("!").head)
    classLoader = new URLClassLoader(Seq(jarURL), getClass.getClassLoader)
    descClass <- loadDescriptor(classLoader, id, metadata)
  } yield id -> DriverTag(id, metadata, descClass, classLoader)

  val initAndStart = (drv: DeviceDriverWrapper) => {
    Try {
      drv.controller.init()
      drv.controller.start()
      drv
    }
    drv
  }

  def availableDrivers: Iterable[DriverMetadata] = driverTags.map(_._2.metadata)

  def instanceDriver(id: String): Option[DeviceDriverWrapper] = synchronized {
    for {
      tag <- driverTags.get(id)
      controller <- compileDriver(tag).toOption
      schemas <- extractTaskSchemas(tag)
    } yield DeviceDriver(controller.configurator, controller, schemas, tag.metadata)
  }

  private def extractTaskSchemas(tag: DriverTag) = {
    ObjectExtractor.overrideClassLoader(tag.classLoader)
    val tryTasks = Try {
      val tasks = tag.descriptor.tasks.map(cls =>
        TaskSchemaFactory.createSchema(runtimeMirror(tag.classLoader).classSymbol(cls).toType))
      tasks
    }
    ObjectExtractor.overrideClassLoader(getClass.getClassLoader)

    tryTasks.fold(
      err => EventBus.trigger(DriverInstantiationError(err, tag.metadata)),
      ok => ())

    tryTasks.toOption
  }

  private def extractMetadata(props: Properties) = {
    val metaCheck = validate(create(props))
    metaCheck.fold(
      err => EventBus.trigger(DriverInvalidMetadataError(err)),
      ok => ())
    metaCheck.toOption
  }

  private def loadDescriptor(cl: ClassLoader, name: String, metadata: DriverMetadata) = {
    val descClazz = Try {
      cl.loadClass(metadata.descriptorClassName).newInstance().asInstanceOf[Driver]
    }
    descClazz.fold(
      err => EventBus.trigger(DriverLoadingError(err, metadata)),
      ok => EventBus.trigger(DriverLoaded(metadata)))
    descClazz.toOption
  }

  def getMetadata(id: String): Option[DriverMetadata] = driverTags.get(id).map(_.metadata)

  private def genSourceForCompilation(tag: DriverTag) = {
    val controllerClass = tag.descriptor.controllerClass.getName
    val configClass = tag.descriptor.configurationClass.getName
    s"""
       | import api.internal.PersistedConfig
       | import api.internal.{DriversManagerV2 => dm}
       | val meta = dm.getMetadata("${tag.id}").get
       | val cfg = new $configClass(meta) with PersistedConfig
       | new $controllerClass(cfg)
         """.stripMargin
  }

  private def compileDriver(tag: DriverTag): Try[DeviceController] = {
    val tryCompile = Try {
      val tb = runtimeMirror(tag.classLoader).mkToolBox()
      (tb eval (tb parse genSourceForCompilation(tag))).asInstanceOf[DeviceController]
    }
    tryCompile fold(
      err => EventBus.trigger(DriverInstantiationError(err, tag.metadata)),
      _ => EventBus.trigger(DriverInstanced(tag.metadata)))

    tryCompile
  }
}
