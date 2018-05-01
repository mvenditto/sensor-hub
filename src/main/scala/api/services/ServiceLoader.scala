package api.services

import java.io.File
import java.nio.file.Paths

import org.apache.xbean.finder.ResourceFinder
import org.slf4j.{Logger, LoggerFactory}
import spi.service.{Service, ServiceMetadata}

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.util.Try

object ServiceLoader {

  var servicesDir = "../ext/services/"
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)
  private[this] val logger: Logger = LoggerFactory.getLogger("services-loader")

  new File(servicesDir)
    .listFiles()
    .filter(_.isDirectory)
    .map(dir => (dir.getName, dir.listFiles().find(_.getName.endsWith(".jar"))))
    .foreach(s => s._2.foreach(
      j => if (j.getName.split('.').head == s._1) cl.addURL(j.toURI.toURL)))

  private val serviceFinder = new ResourceFinder("META-INF/", cl)

  val services: Map[String, Class[_ <: Service]] =
    serviceFinder.mapAllImplementations(classOf[Service]).asScala.toMap

  def runAllServices(): Unit = services.foreach(service => {
    logger.info(s"loading service: ${service._1}")
    Try(service._2.newInstance().init(ServiceMetadata(service._1, "", "", Paths.get(servicesDir, service._1).toString)))
      .fold(err => logger.error(s"error loading ${service._1}: ${err.getMessage}"), _ => ())
  })

}
