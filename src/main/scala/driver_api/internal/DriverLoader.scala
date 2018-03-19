package driver_api.internal

import java.io.File
import java.util.jar.JarFile

import driver_api._
import driver_api.annotation.DriverAnnotations.Metadata
import fi.oph.myscalaschema.extraction.ObjectExtractor
import fi.oph.myscalaschema.{Schema, SchemaFactory}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox
import scala.util.Try
import scala.util.matching.Regex


object DriverLoader {

  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)
  val mirror = runtimeMirror(cl)
  lazy val tb = mirror.mkToolBox()

  private val className = "className"
  private val driverRootPackage = "driver"
  private val driverClassPattern = new Regex("""driver/([A-Za-z]*).class""", className)
  private val driverMsgClassPattern = new Regex("""(driver/messages/[A-Za-z]*).class""", className)

  private[this] val logger = LoggerFactory.getLogger("driver-manager")

  ObjectExtractor.overrideClassLoader(cl)

  private def loadJar(jar: File): Unit = {
    logger.debug(s"loading jar: ${jar.getName}")
    cl.addURL(jar.toURI.toURL)
  }

  private def extractMetadata(jar: File): Iterable[Metadata] = {
    logger.debug(s"extracting metadata from: ${jar.getName}")
    for {
      clsName <- driverJarClasses(jar)
      c = cl.loadClass(s"$driverRootPackage.$clsName")
      annotation <- runtimeMirror(cl).classSymbol(c).annotations
      tree = annotation.tree
      if tree.tpe <:< typeOf[Metadata]
      _ = logger.debug(s"entry class $clsName @ ${jar.getName}")
      a =  tb.eval(tb.untypecheck(tree)).asInstanceOf[Metadata]
    } yield a
  }

  private def driverJarClasses(jar: File): Iterable[String] = {
    for {
      entry <- new JarFile(jar).entries().asScala.toList
      je = driverClassPattern.findFirstMatchIn(entry.getName)
      if je.nonEmpty
      clsName = je.get.group(className)
    } yield clsName
  }

  def driverMessageSchemas(jar: File): Map[Class[_], Schema] = {
    var classes = Seq.empty[Class[_]]
    val schemas = for {
      entry <- new JarFile(jar).entries().asScala.toList
      je <- driverMsgClassPattern.findFirstMatchIn(entry.getName)
      clsName = je.group(className).replace("/", ".")
      cls <- Try(cl.loadClass(clsName)).toOption
      clsSym = mirror.classSymbol(cls)
      if clsSym.annotations.exists(_.tree.tpe <:< typeOf[Msg])
      schema = SchemaFactory.default.createSchema(clsSym.toType)
      _ = logger.debug(s"loaded @Msg from $clsName @ ${jar.getName}")
      _ = classes :+= cls
    } yield schema

    classes.zip(schemas).toMap
  }

  private def compileDriverWithObservables(ctrlClass: Class[_],
    configClass: Class[_]): DeviceController with ObservablesSupport = {
    logger.debug(s"compiling driver: ${ctrlClass.getCanonicalName}|${configClass.getCanonicalName}")
    tb.eval(tb.parse(
      s"""
      val cfg = new ${configClass.getCanonicalName};
      val ctrl = new ${ctrlClass.getCanonicalName}(cfg) with driver_api.ObservablesSupport;
      ctrl""")).asInstanceOf[DeviceController with ObservablesSupport]
  }

  def loadDriverFromJar(jar: File): Option[DeviceDriverWrapper] = {
    loadJar(jar)
    for {
      metadata <- extractMetadata(jar).headOption
      configClass = cl.loadClass(metadata.configClass)
      ctrlClass = cl.loadClass(metadata.controllerClass)
      controller_ = compileDriverWithObservables(ctrlClass, configClass)
    } yield DeviceDriver(controller_.configurator, controller_)
  }

  def dispose(): Unit = cl.close()

}
