package driver_api.internal

import scala.util.matching.Regex

object DriverConstants {

  val DriverPackage = "driver"
  val MetadataFile = "metadata.conf"
  val JniLibraryDir = "jniLibs"
  val MessagesPackage = "messages"
  val StorageDir = "files"

  val MessageClassNameKey = "className"
  val MessageClassPattern = new Regex(s"""($DriverPackage/$MessagesPackage/[A-Za-z]*).class""", MessageClassNameKey)

}
