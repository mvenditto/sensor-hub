package api.services

import java.io.File
import java.nio.file.Paths

import api.config.Preferences
import api.events.EventBus
import api.events.SensorsHubEvents.{ServiceLoaded, ServiceLoadingError}
import org.apache.xbean.finder.ResourceFinder
import org.slf4j.LoggerFactory
import spi.service.{Service, ServiceMetadata}

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ServicesManager {

  private[this] val servicesDir = Preferences.cfg.servicesDir
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)

  private[this] var _registeredServices = Seq.empty[(ServiceMetadata,Service)]
  private[this] val logger = LoggerFactory.getLogger("sh.services-manager")

  Option(new File(servicesDir)).fold(logger.error(s"services directory ($servicesDir) does not exists!")) {
    _.listFiles()
      .filter(_.isDirectory)
      .map(dir => (dir.getName, dir.listFiles().find(_.getName.endsWith(".jar"))))
      .foreach(s => s._2.foreach(
        j => if (j.getName.split('.').head == s._1) cl.addURL(j.toURI.toURL)))
  }

  private val serviceFinder = new ResourceFinder("META-INF/", cl)

  val services: Map[String, Class[_ <: Service]] =
    serviceFinder.mapAllImplementations(classOf[Service]).asScala.toMap

  def registeredServices: Seq[ServiceMetadata] = _registeredServices.map(_._1)

  def shutdownService(name: String): Future[Unit] = Future {
    val service = _registeredServices.find(_._1.name == name).map(_._2)
    service.foreach(s => {
      s.stop()
      s.dispose()
      _registeredServices = _registeredServices.filter(_._1.name != name)
    })
  }

  def runAllServices(): Future[List[Unit]] = {
    Future.sequence(services.map(service => {
      val meta = ServiceMetadata(service._1, "", "", Paths.get(servicesDir, service._1).toString)
      val serviceInstance = service._2.newInstance()
      Future {
        Try {
          Thread.currentThread().setContextClassLoader(cl)
          serviceInstance.init(meta)
          serviceInstance.start()
          serviceInstance
        }.fold(
          err => EventBus.trigger(ServiceLoadingError(err, meta)),
          _ => {
            _registeredServices :+= (meta, serviceInstance)
            EventBus.trigger(ServiceLoaded(meta))
          })
      }
    }).toList)
  }
}
