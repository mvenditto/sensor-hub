package utils

import java.io.File
import java.nio.file._
import java.util.zip.ZipFile

import scala.util.Try
import scala.collection.JavaConverters._

object ZipUtils {

  def extractFile(src: Path, dst: Path, filePath: String): Try[File] = {
   Try {
     val fs = FileSystems.newFileSystem(src, null)
     val fileName = filePath.split('/').last
     val fileToExtract = fs.getPath(filePath)
     val out = new File(dst.toFile, fileName)
     Files.copy(fileToExtract, Paths.get(dst.toString, fileName), StandardCopyOption.REPLACE_EXISTING)
     out
   }
  }

  def extractTempFile(src: Path, dstPrefix: String, fileName: String): Try[(File,File)] = {
    Try {
      val tmpDir = createTempDir(dstPrefix).get
      tmpDir -> extractFile(src, tmpDir.toPath, fileName).get
    }
  }

  def extractFiles(src: Path, dst: Path, fileNames: Seq[String]): Iterable[Try[File]] =
    fileNames.map(extractFile(src, dst, _))


  def extractTempFiles(src: Path, dstPrefix: String, fileNames: Seq[String]): Iterable[Try[(File,File)]] =
    fileNames.map(extractTempFile(src, dstPrefix, _))

  def createTempDir(name: String, shouldDeleteOnExit:Boolean = true): Try[File] = {
    Try {
      val tmpDir = new File(System.getProperty("java.io.tmpdir"), name)
      tmpDir.mkdir()
      if (shouldDeleteOnExit) deleteOnExit(tmpDir) else tmpDir
    }
  }

  def deleteOnExit(file: File): File = {
    file.deleteOnExit()
    file
  }

  def list(src: ZipFile, dir: String): Try[Seq[String]] = {
    Try {
      src.stream().iterator().asScala
        .filter(e => !e.isDirectory && e.getName.startsWith(dir))
        .map(_.getName).toSeq
    }
  }
}
