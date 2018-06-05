package api.internal

import java.io.File

import api.devices.Sensors._
import org.json4s.Formats
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.jackson.JsonMethods.{pretty, parse}
import utils.CustomSeriDeseri

import scala.io.Source


trait JsonManifest {

  private implicit val fmts: Formats = CustomSeriDeseri.fmt

  private def extractProcedure(dc: DeviceController, procName: String): (DataStream) => Observation = {
    val method = dc.getClass.getDeclaredField(procName)
    method.setAccessible(true)
    method.get(dc).asInstanceOf[(DataStream) => Observation]
  }

  def dataStreamsFromManifest(dc: DeviceController, manifestPath: String): Seq[DataStream] = {
    val src = Source.fromFile(new File(manifestPath)).mkString
    val json = parse(src)

    for {
      JObject(child) <- json \ "datastreams"
      JField("name", JString(name)) <- child
      JField("description", JString(desc)) <- child
      JField("observationType", JString(obsType)) <- child
      JField("observedProperty", obsProp) <- child
      JField("featureOfInterest", fov) <- child
      JField("unitOfMeasurement", uom) <- child
      JField("procedure", JString(procName)) <- child
    } yield DataStream(name, desc,
      uom.extract[UnitOfMeasurement],
      fov.extract[FeatureOfInterest],
      ObservationTypes.fromName(obsType),
      obsProp.extract[ObservedProperty],
      extractProcedure(dc,procName))
  }
}

trait DeviceController extends JsonManifest {

  val configurator: DeviceConfigurator

  lazy val _dataStreams = dataStreamsFromManifest(this, configurator.metadata.rootDir)

  def dataStreams: Iterable[DataStream] = _dataStreams

  def init(): Unit

  def start(): Unit

  def stop(): Unit

}