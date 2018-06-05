package api.internal

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Paths
import java.util.Properties
import java.util.jar.JarFile

import api.config.Preferences
import api.devices.Devices.Device
import api.devices.DevicesManager
import api.devices.Sensors.Encodings
import api.events.{EventBus, EventLogging}
import api.events.SensorsHubEvents._
import api.internal.MetadataFactory._
import api.internal.MetadataValidation._
import api.tasks.TaskSchema
import api.tasks.oph.TaskSchemaFactory
import io.reactivex.subjects.PublishSubject
import spi.drivers.Driver
import org.apache.log4j.BasicConfigurator
import org.apache.xbean.finder.ResourceFinder

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.reflect.runtime.universe._
import scala.util.Try

object testV2 extends App {


  import DriversManager.initAndStart

  BasicConfigurator.configure()
  Preferences.configure("sh-prefs.conf")
  EventLogging.init()

  var dev = DriversManager
    .instanceDriver("dummy-therm-driver")
    .map(initAndStart)
    .map(drv =>
      DevicesManager.createDevice("dev", "", Encodings.PDF, new URI(""), drv)
    )
    .get

  dev.dataStreams.head.observable.subscribe(obs => println(obs))
  println("paused (5 s.)")
  Thread.sleep(10000)
  DriversManager.reloadDriver("therm1")
  /*dev.updateWith(newDriver = DriversManager
    .instanceDriver("dummy-therm-driver")
    .map(initAndStart).get)*/
  println("ok")

}

object DriversManager{

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
  private[this] var tasksCache = Map.empty[String, List[TaskSchema]]

  private[this] var driverTags = for {
    props <- finder.mapAllProperties(uri).asScala
    url <- finder.getResourcesMap(uri).asScala
    metadata <- extractMetadata(props._2)
    id = props._1
    jarURL = new URL(url._2.getFile.split("!").head)
    classLoader = new URLClassLoader(Seq(jarURL), getClass.getClassLoader)
    descClass <- loadDescriptor(classLoader, id, metadata)
  } yield id -> DriverTag(id, metadata, descClass, classLoader)

  def availableDrivers: Iterable[DriverMetadata] = driverTags.map(_._2.metadata)

  def tagFor(name: String): Option[DriverTag] =
    driverTags.find(_._2.metadata.name equals name).map(_._2)

  def instanceDriver(name: String): Option[DeviceDriverWrapper] = synchronized {
    for {
      tag <- driverTags.find(_._2.metadata.name equals name).map(_._2)
      (controller, configurator) <- compileDriver(tag).toOption
      schemas <- extractTaskSchemas(tag)
    } yield DeviceDriver(configurator, controller, schemas, tag.metadata)
  }

   def reloadDriver(id: String): Unit = {
     var updated = Seq.empty[DriverMetadata]
     driverTags = driverTags.map(tag => {
      if (tag._1 equals id) {
        val x: Option[DriverTag] = finder.getResourcesMap(uri).asScala.get(id).map(url => {
          val jarURL = new URL(url.getFile.split("!").head)
          val classLoader = new URLClassLoader(Seq(jarURL), getClass.getClassLoader)
          val descClass = loadDescriptor(classLoader, id, tag._2.metadata)
          if (descClass.isDefined) {
            updated :+= tag._2.metadata
            DriverTag(tag._1, tag._2.metadata, descClass.get, classLoader)
          }
          else tag._2
        })
        tag._1 -> x.get
      } else tag
    })
    updated.foreach(metadata => EventBus.trigger(DriverChanged(metadata)))
  }

  private def extractTaskSchemas(tag: DriverTag) = {

    val tasks = () => {
      val tasks_ = tag.descriptor.tasks.map(cls => {
        tag.classLoader.loadClass(cls.getName)
        TaskSchemaFactory.createSchema(
          runtimeMirror(tag.classLoader).classSymbol(cls).toType,
          tag.classLoader
        )
      })
      tasksCache = tasksCache + (tag.id -> tasks_)
      tasks_
    }

    val tryTasks = Try(tasksCache.getOrElse(tag.id, tasks()))

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

  private def compileDriver(tag: DriverTag): Try[(DeviceController, DeviceConfigurator)] = {
    val tryCompile = Try {
      val configurator =
        tag.descriptor.configurationClass.getConstructors.head.newInstance(Seq(tag.metadata):_*).asInstanceOf[AnyRef]
      val controller =
        tag.descriptor.controllerClass.getConstructors.head.newInstance(Seq(configurator):_*)
            .asInstanceOf[DeviceController]
      controller -> new DevConfigAdapter(configurator.asInstanceOf[DeviceConfigurator]) with PersistedConfig
    }
    tryCompile fold(
      err => EventBus.trigger(DriverInstantiationError(err, tag.metadata)),
      _ => EventBus.trigger(DriverInstanced(tag.metadata)))
    tryCompile
  }

  val initAndStart = (drv: DeviceDriverWrapper) => {
    Try {
      drv.controller.init()
      drv.controller.start()
      drv
    }
    drv
  }
}
