package api.internal

import java.util.Properties

case class DriverMetadata(
  name: String,
  version: String,
  description: String,
  descriptorClassName: String
)

object MetadataValidation {

  import MetadataFactory._

  private val missing: (String) => ValidationError = (fieldName: String) =>
    ValidationError(s"""$fieldName is missing!""")

  case class ValidationError(msg: String) extends Exception(msg) {
    override lazy val toString: String = msg
  }

  val validate: (DriverMetadata) => Either[ValidationError, DriverMetadata] = (metadata: DriverMetadata) => {
    if (metadata.name == Unknown) missing(Name)
    if (metadata.descriptorClassName == Unknown) missing(Name)
    Right(metadata)
  }

}

object MetadataFactory {

  val Unknown = "unknown"
  val Name = "name"
  val Version = "version"
  val Description = "description"
  val ClassName = "className"

  val create: (Properties) => DriverMetadata = (p: Properties) => {
      DriverMetadata(
        p.getProperty(Name, Unknown),
        p.getProperty(Version, Unknown),
        p.getProperty(Description, Unknown),
        p.getProperty(ClassName, Unknown)
      )
  }

}
