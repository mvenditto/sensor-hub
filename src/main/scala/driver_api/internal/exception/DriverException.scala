package driver_api.internal.exception

object DriverException {

  sealed class DriverException(msg: String) extends Exception(msg)

  class MissingMetadataException(msg: String) extends DriverException(msg)
  class MetadataParsingException(msg: String) extends DriverException(msg)
  class NativeLibraryDumpException(msg: String) extends DriverException(msg)
  class DriverClassLoadingException(msg: String) extends DriverException(msg)
  class DriverInterfacesDumpException(msg: String) extends DriverException(msg)
}
