package api.internal

import java.io.File
import java.nio.file.Paths
import java.util.Properties

import api.config.Preferences
import api.sensors.Sensors._
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source
import utils.CustomSeriDeseri.fmt

case class DriverMetadata(
  name: String,
  version: String,
  description: String,
  descriptorClassName: String,
  datastreamsMetadata: Map[String, DataStreamMetadata]
) {
  val rootDir: String = Paths.get(Preferences.cfg.driversDir, name+".json").toString
}

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
    val md = DriverMetadata(
        p.getProperty(Name, Unknown),
        p.getProperty(Version, Unknown),
        p.getProperty(Description, Unknown),
        p.getProperty(ClassName, Unknown),
        null)
    md.copy(datastreamsMetadata = datastreamsMetadata(md.rootDir))
  }

  def datastreamsMetadata(rootDir: String): Map[String, DataStreamMetadata] = {
    val src = Source.fromFile(new File(rootDir)).mkString
    val json = parse(src)
    val ds: List[(String, String, UnitOfMeasurement, FeatureOfInterest, ObservationType, ObservedProperty)] = for {
      JObject(child) <- json \ "datastreams"
      JField("name", JString(name)) <- child
      JField("description", JString(desc)) <- child
      JField("observationType", JString(obsType)) <- child
      JField("observedProperty", obsProp) <- child
      JField("featureOfInterest", fov) <- child
      JField("unitOfMeasurement", uom) <- child
    } yield (name, desc,
      uom.extract[UnitOfMeasurement],
      fov.extract[FeatureOfInterest],
      ObservationTypes.fromName(obsType),
      obsProp.extract[ObservedProperty])

    Map(ds.map(d => d._1 -> DataStreamMetadata(d._2, d._3, d._4, d._5, d._6)): _*)
  }
}
