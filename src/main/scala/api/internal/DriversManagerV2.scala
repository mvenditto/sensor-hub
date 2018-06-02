package api.internal

import java.io.File
import java.net.URL
import java.util.Properties

import api.config.Preferences
import api.events.{EventBus, EventLogging}
import api.events.SensorsHubEvents._
import api.internal.MetadataFactory._
import api.internal.MetadataValidation._
import api.tasks.TaskSchema
import api.tasks.oph.TaskSchemaFactory
import spi.drivers.Driver
import org.apache.log4j.BasicConfigurator
import org.apache.xbean.finder.ResourceFinder

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.reflect.runtime.universe._
import scala.util.Try

object testV2 extends App {


  import DriversManagerV2.initAndStart

  BasicConfigurator.configure()
  Preferences.configure("sh-prefs.conf")
  EventLogging.init()

  DriversManagerV2
    .instanceDriver("therm1")
    .map(initAndStart).foreach {
      drv =>
        println(drv.controller.dataStreams.head.doObservation().result)
        println(drv.controller.asInstanceOf[DeviceController with TaskingSupport]
          .send("dummy-task", """{"message":"prova"}""").blockingGet())
    }
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
  private[this] var tasksCache = Map.empty[String, List[TaskSchema]]

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

  def tagFor(name: String): Option[DriverTag] =
    driverTags.find(_._2.metadata.name equals name).map(_._2)

  def instanceDriver(id: String): Option[DeviceDriverWrapper] = synchronized {
    for {
      tag <- driverTags.get(id)
      (controller, configurator) <- compileDriver(tag).toOption
      schemas <- extractTaskSchemas(tag)
    } yield DeviceDriver(configurator, controller, schemas, tag.metadata)
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
}
