package api.services

import java.io.File
import java.nio.file.Paths

import api.events.EventBus
import api.events.SensorsHubEvents.{ServiceLoaded, ServiceLoadingError}
import org.apache.xbean.finder.ResourceFinder
import org.slf4j.{Logger, LoggerFactory}
import spi.service.{Service, ServiceMetadata}

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ServicesManager {

  var servicesDir = "../ext/services/"
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)
  private[this] val logger: Logger = LoggerFactory.getLogger("sh.services-loader")

  private[this] var _registeredServices = Seq.empty[ServiceMetadata]

  new File(servicesDir)
    .listFiles()
    .filter(_.isDirectory)
    .map(dir => (dir.getName, dir.listFiles().find(_.getName.endsWith(".jar"))))
    .foreach(s => s._2.foreach(
      j => if (j.getName.split('.').head == s._1) cl.addURL(j.toURI.toURL)))

  private val serviceFinder = new ResourceFinder("META-INF/", cl)

  val services: Map[String, Class[_ <: Service]] =
    serviceFinder.mapAllImplementations(classOf[Service]).asScala.toMap

  def registeredServices: Seq[ServiceMetadata] = _registeredServices

  def runAllServices(): Future[List[Unit]] = {
    Future.sequence(services.map(service => {
      logger.info(s"loading service: ${service._1}")
      val meta = ServiceMetadata(service._1, "", "", Paths.get(servicesDir, service._1).toString)
      val serviceInstance = service._2.newInstance()
      Future {
        Try {
          serviceInstance.init(meta)
          new Thread(() => serviceInstance.start()).start()
          serviceInstance
        }.fold(
          err => EventBus.trigger(ServiceLoadingError(err, meta)),
            // logger.error(s"error loading ${service._1}: ${err.getMessage}"),
          s => {
            _registeredServices :+= meta
            EventBus.trigger(ServiceLoaded(meta))
            //logger.info(s"loaded service: ${service._1} -> $s")
          })
      }
    }).toList)
  }

  def runAllServices2(): Future[List[Unit]] = Future.sequence(services.map(service => {
        val meta = ServiceMetadata(service._1, "", "", Paths.get(servicesDir, service._1).toString)
        Future {
          Try(service._2.newInstance().init(meta))
            .fold(
              err =>
                EventBus.trigger(ServiceLoadingError(err, meta)),
                // logger.error(s"error loading ${service._1}: ${err.getMessage}"),
              _ => {
                EventBus.trigger(ServiceLoaded(meta))
                _registeredServices :+= meta
              })
        }
      }
  ).toList)

}
