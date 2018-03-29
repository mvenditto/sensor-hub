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
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex
import pureconfig._
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}
import utils.ZipUtils._
import utils.Implicits._
import DriverConstants._
import driver_api.internal.exception.DriverException._


object Test extends App {

  /*
  val path = "/home/pps/electrom/scalajs-electron-skeleton/verlet/verlet/sensors-hub/out/artifacts/snowboy_detect_jar/"
  val jarName = "snowboy-detect.jar"
  val cfgName = "hotword_detection.conf"
  val jarFile = new File(path + jarName)

  val drv = DriverManager.loadFromJar(jarFile)
  println(drv)
  drv.foreach(d => {
    d.config.configure(path + cfgName)
    d.controller.init()
    d.controller.asInstanceOf[DeviceController with EventEmitter]
      .emitters("hotwords").subscribe(hw => println(hw))
    d.controller.start()
  })*/

  val path = "/home/pps/electrom/scalajs-electron-skeleton/verlet/verlet/sensors-hub/out/artifacts/dummy_lcd_display_driver_jar/"
  val jarName = "dummy-lcd-display-driver.jar"
  val cfgName = "conf.conf"
  val jarFile = new File(path + jarName)
  val drv = DriverManager.loadFromJar(jarFile)
  println(drv)
  drv.foreach(d => {
    d.config.configure(path + cfgName)
    d.controller.init()
    //d.controller.propertyStreams("temperature0").subscribe(obs => println(obs))
    d.controller.start()
    Thread.sleep(2000)
    d.controller.asInstanceOf[MessageSupport].send("""{"message":"newText"}""")
  })
}

object DriverManager {

  val cl = new ScalaClassLoader.URLClassLoader(Seq.empty, getClass.getClassLoader)
  private val mirror = runtimeMirror(cl)
  private lazy val tb = mirror.mkToolBox()
  private val TmpFolderName = "sensor-hub-tmp"
  private val tmpFolder = deleteOnExit(createTempDir(TmpFolderName).get)

  org.apache.log4j.BasicConfigurator.configure() // dirty log4j conf for debug purpose TODO
  private val logger = LoggerFactory.getLogger("driver-manager")

  ObjectExtractor.overrideClassLoader(cl)

  private def addToClassLoader(jar: File): Unit = {
    logger.info(s"adding ${jar.toPath} to the classLoader")
    cl.addURL(jar.toURI.toURL)
  }

  private def tryExtractJniLibs(driver: JarDriver, root: Path): Try[Unit] = {
    logger.info(s"extracting jniLibs @ $root/jniLibs")
    Try {
      deleteOnExit(Files.createDirectory(Paths.get(root, JniLibraryDir)).toFile)
      val jniLibs = driver.extractJniLibs()
      if (jniLibs.forall(_.isSuccess)) {
        jniLibs.foreach(_.get.deleteOnExit())
        Success()
      }
      else Failure(jniLibs.collectFirst{ case Failure(x) => x }
        .getOrElse(new NativeLibraryDumpException("an error occurred while extracting native libs.")))
    }
  }

  private def createDriverTmpDirs(jar: File): Try[Path] = {
    val path = Paths.get(tmpFolder.getAbsolutePath, jar.getName.replace('.', '_'))
    logger.info(s"creating tmp directory for driver @ $path")
    Try{
      val dir = Files.createDirectory(path)
      dir.toFile.deleteOnExit()
      dir
    }
  }

  private def compileDriverWithObservables(
    ctrlClass: Class[_], configClass: Class[_], nativeLibsPath: String): Try[DeviceController with ObservablesSupport] = {
    Try {
      logger.info(s"compiling driver: ${ctrlClass.getCanonicalName}|${configClass.getCanonicalName}")
      tb.eval(tb.parse(
        s"""
        val cfg = new ${configClass.getCanonicalName};
        cfg.setJniLibPath("$nativeLibsPath");
        val ctrl = new ${ctrlClass.getCanonicalName}(cfg) with driver_api.ObservablesSupport;
        ctrl
        """)).asInstanceOf[DeviceController with ObservablesSupport]
    }
  }

  def loadFromJar(jar: File): Try[DeviceDriverWrapper] = {
    for {
      // create this driver tmp directories
      root <- createDriverTmpDirs(jar)
      // try instance the driver, could fail (eg. no metadata.conf in jar root)
      driver <- JarDriverFactory.tryCreate(jar, root)
      // load the jar into the classloader
      _ = addToClassLoader(driver.jar)
      // delete metadata.conf on exit
      _ = driver.metadataFile.deleteOnExit()
      // extract jni libs if any exists (deleted on exit)
      _ <- tryExtractJniLibs(driver, root)
      commands <- driver.commandClasses(cl)
      // get this driver controller class
      controllerCls <- driver.controllerClass(cl)
      // get this driver configuration class
      configCls <- driver.configurationClass(cl)
      // create a new instance of the controller
      controller <- compileDriverWithObservables(
        controllerCls, configCls, Paths.get(root, JniLibraryDir))
      // if all steps were Ok yield a usable DeviceDriver
    } yield DeviceDriver(controller.configurator, controller)
  }
}