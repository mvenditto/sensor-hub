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
    .filter(_.getName.endsWith(".jar"))
    .map(_.toURI.toURL)
    .foreach(cl.addURL)

  private val serviceFinder = new ResourceFinder("META-INF/", cl)

  val services: Map[String, Class[_ <: Service]] =
    serviceFinder.mapAllImplementations(classOf[Service]).asScala.toMap

}

object TestServicess extends App {
  println(DriversManager.availableDrivers)
  //ServiceLoader.services.head._2.newInstance().init(DevicesManager.obsBus)
}
