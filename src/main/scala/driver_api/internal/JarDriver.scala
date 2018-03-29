package driver_api.internal

import java.io.File
import java.nio.file.{Path, Paths}
import java.util.jar.JarFile
import java.util.zip.ZipFile

import driver_api.Msg
import driver_api.annotation.DriverAnnotations.Metadata
import driver_api.internal.DriverConstants._
import driver_api.internal.exception.DriverException._
import fi.oph.myscalaschema.SchemaFactory
import utils.DriverUtils._
import utils.Implicits._
import utils.ZipUtils._

import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

sealed trait JarDriver {

  val jar: File

  val rootDir: Path

  val metadata: Metadata

  val metadataFile: File

  def extractJniLibs(): Iterable[Try[File]]

  def extractStorage(): Try[File]

  def controllerClass(cl: ClassLoader): Try[Class[_]]

  def configurationClass(cl: ClassLoader): Try[Class[_]]

  def commandClasses(cl: ClassLoader): Try[Iterable[Class[_]]]

}


private[internal] class DefaultJarDriver(
  override val jar: File,
  override val rootDir: Path
) extends JarDriver {

  val (metadataFile: File, metadata: Metadata) = tryExtractMetadata()

  lazy val libDir: Path = Paths.get(rootDir, JniLibraryDir)
  lazy val storageDir: Path = Paths.get(rootDir, StorageDir)

  def extractJniLibs(): Iterable[Try[File]] = {
    extractFiles(jar.toPath, libDir, list(new ZipFile(jar), JniLibraryDir).get.toList) map {
      case Success(r) =>
        Success(r)
      case Failure(cause) =>
        Failure(new NativeLibraryDumpException(cause.getMessage))
    }
  }

  def extractStorage(): Try[File] = { throw new UnsupportedOperationException() }

  def controllerClass(cl: ClassLoader): Try[Class[_]] =
    loadJarClass(metadata.controllerClass, cl)

  def configurationClass(cl: ClassLoader): Try[Class[_]] =
    loadJarClass(metadata.configClass, cl)

  def commandClasses(cl: ClassLoader): Try[Iterable[Class[_]]] = {
    var classes = Seq.empty[Class[_]]
    val mirror = scala.reflect.runtime.universe.runtimeMirror(cl)
    import scala.reflect.runtime.universe.typeOf
    Try {
      val schemas = for {
        entry <- new JarFile(jar).entries().asScala.toList
        je <- MessageClassPattern.findFirstMatchIn(entry.getName)
        clsName = je.group(MessageClassNameKey).replace("/", ".")
        cls <- Try(cl.loadClass(clsName)).toOption
        clsSym = mirror.classSymbol(cls)
        if clsSym.annotations.exists(_.tree.tpe <:< typeOf[Msg])
        schema = SchemaFactory.default.createSchema(clsSym.toType)
        _ = println(s"loaded @Msg from $clsName @ ${jar.getName}")
        _ = classes :+= cls
      } yield schema
      classes
    }
  }


  /* = {
    Try {
      for {
        entry <- new JarFile(jar).entries().asScala.toList
        msgMatch = MessageClassPattern.findFirstMatchIn(entry.getName)
        if msgMatch.nonEmpty
        clsName = msgMatch.get.group(MessageClassNameKey).replace("/", ".")
        cls = cl.loadClass(clsName)
      } yield cls
    }
  }*/

  private def loadJarClass(className: String, cl: ClassLoader): Try[Class[_]] = {
    Try(cl.loadClass(className)) match {
      case Success(clazz) => Success(clazz)
      case Failure(cause) => Failure(new DriverClassLoadingException(cause.getLocalizedMessage))
    }
  }

  private def tryExtractMetadata(): (File,Metadata) = {
    extractFile(jar.toPath, rootDir, MetadataFile) match {
      case Success(metadataFile) =>
        parseMetadata(metadataFile)
      case Failure(cause) =>
        throw new MissingMetadataException(cause.getMessage)
    }
  }

  private def parseMetadata(md: File): (File, Metadata) = tryParseMetadata(md) match {
    case Success(meta) =>
      md -> meta
    case Failure(cause) =>
      throw new MetadataParsingException(cause.getMessage)
  }
}

object JarDriverFactory {

  def tryCreate(jar: File, rootDir: Path): Try[JarDriver] =
    Try(new DefaultJarDriver(jar, rootDir))

}
