package api.services

import java.io.File
import java.nio.file.Paths

import api.events.EventBus
import api.events.SensorsHubEvents.{ServiceLoaded, ServiceLoadingError}
import org.apache.xbean.finder.ResourceFinder
import spi.service.{Service, ServiceMetadata}

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ServicesManager {

  var servicesDir = "../ext/services/"
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)

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
      val meta = ServiceMetadata(service._1, "", "", Paths.get(servicesDir, service._1).toString)
      val serviceInstance = service._2.newInstance()
      Future {
        Try {
          serviceInstance.init(meta)
          new Thread(() => serviceInstance.start()).start()
          serviceInstance
        }.fold(
          err => EventBus.trigger(ServiceLoadingError(err, meta)),
          _ => {
            _registeredServices :+= meta
            EventBus.trigger(ServiceLoaded(meta))
          })
      }
    }).toList)
  }
}
