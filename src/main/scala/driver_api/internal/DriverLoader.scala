package driver_api.internal

import java.io.File
import java.nio.file._
import java.util.jar.JarFile
import java.util.zip.ZipFile

import driver_api._
import driver_api.annotation.DriverAnnotations.Metadata
import fi.oph.myscalaschema.extraction.ObjectExtractor
import fi.oph.myscalaschema.{Schema, SchemaFactory}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox
import scala.util.{Success, Try}
import scala.util.matching.Regex
import pureconfig._
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}
import utils.ZipUtils._

object DriverLoader {
  org.apache.log4j.BasicConfigurator.configure()
  private[this] implicit def camelCaseHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)
  val mirror = runtimeMirror(cl)
  lazy val tb = mirror.mkToolBox()

  private val className = "className"
  private val driverRootPackage = "driver"
  private val tmpFolder = "sensor_hub_tmp"
  private val driverClassPattern = new Regex("""driver/([A-Za-z]*).class""", className)
  private val driverMsgClassPattern = new Regex("""(driver/messages/[A-Za-z]*).class""", className)

  private[this] val logger = LoggerFactory.getLogger("driver-manager")

  ObjectExtractor.overrideClassLoader(cl)
  createTempDir(tmpFolder).map(deleteOnExit)

  private def loadJar(jar: File): Unit = {
    logger.debug(s"loading jar: ${jar.getName}")
    cl.addURL(jar.toURI.toURL)
  }

  private def extractMetadata(jar: File): Option[Metadata] = {

    val tryExtract = extractTempFile(
      jar.toPath, Paths.get(tmpFolder, jar.getName.replace('.', '_')).toString, "metadata.conf")

    tryExtract toOption match {
      case Some((jarTmpDir, metadata)) =>
        jarTmpDir.deleteOnExit()
        metadata.deleteOnExit()
        logger.info(s"OK read metadata $metadata")
        loadConfigFromFiles[Metadata](Seq(metadata.toPath)).toOption
      case _ =>
        None
    }
  }

  private def extractJniLibs(jar: File): Unit = {
    val jniLibs = Paths.get(tmpFolder, jar.getName.replace('.', '_'), "jniLibs").toString
    extractTempFiles(jar.toPath, jniLibs, list(new ZipFile(jar), "jniLibs").get.toList) foreach {
      case Success((path, lib)) =>
        path.deleteOnExit()
        lib.deleteOnExit()
        logger.info(s"extracted jniLib $lib")
      case _ =>
    }
  }

  private def extractMetadata2(jar: File): Iterable[Metadata] = {
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

  private def compileDriverWithObservables(
    ctrlClass: Class[_],
    configClass: Class[_],
    nativeLibsPath: String
  ): DeviceController with ObservablesSupport = {
    logger.debug(s"compiling driver: ${ctrlClass.getCanonicalName}|${configClass.getCanonicalName}")
    tb.eval(tb.parse(
      s"""
      val cfg = new ${configClass.getCanonicalName};
      cfg.setJniLibPath("$nativeLibsPath");
      val ctrl = new ${ctrlClass.getCanonicalName}(cfg) with driver_api.ObservablesSupport;
      ctrl
      """)).asInstanceOf[DeviceController with ObservablesSupport]
  }

  def loadDriverFromJar(jar: File): Option[DeviceDriverWrapper] = {
    for {
      metadata <- extractMetadata(jar)
      _ = loadJar(jar)
      _ = extractJniLibs(jar)
      configClass = cl.loadClass(metadata.configClass)
      ctrlClass = cl.loadClass(metadata.controllerClass)
      jniLibsPath = Paths.get(System.getProperty("java.io.tmpdir"), tmpFolder, jar.getName.replace('.', '_'), "jniLibs").toString
      controller_ = compileDriverWithObservables(ctrlClass, configClass, jniLibsPath)
    } yield DeviceDriver(controller_.configurator, controller_)
  }


  def dispose(): Unit = cl.close()

}

object Test extends App {

  val path = "/home/pps/electrom/scalajs-electron-skeleton/verlet/verlet/sensors-hub/out/artifacts/snowboy_detect_jar/"
  val jarName = "snowboy-detect.jar"
  val cfgName = "hotword_detection.conf"


  val drv = DriverLoader.loadDriverFromJar(new File(path + jarName))

  drv.foreach(d => {
    d.config.configure(path + cfgName)
    val ctrl = d.controller.asInstanceOf[DeviceController with EventEmitter]
    ctrl.init()
    ctrl.emitters("hotwords").subscribe(hw => println(hw))
    ctrl.start()
  })

}