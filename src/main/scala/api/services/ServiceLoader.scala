package api.services

import java.io.File

import api.internal.DriversManager
import api.sensors.DevicesManager
import org.apache.xbean.finder.ResourceFinder
import spi.service.Service

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader

object ServiceLoader {

  var driversDir = "../ext/services/"
  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)

  new File(driversDir)
    .listFiles()
    .filter(_.isDirectory)
    .map(dir => (dir.getName, dir.listFiles().find(_.getName.endsWith(".jar"))))
    .foreach(s => s._2.foreach(
      j => if (j.getName.split('.').head == s._1) cl.addURL(j.toURI.toURL)))

  private val serviceFinder = new ResourceFinder("META-INF/", cl)

  val services: Map[String, Class[_ <: Service]] =
    serviceFinder.mapAllImplementations(classOf[Service]).asScala.toMap

}
