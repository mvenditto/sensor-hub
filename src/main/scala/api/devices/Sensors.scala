package api.devices

import java.net.URI
import java.time.Period

import api.devices.Devices.Device
import api.internal.Observations
import io.reactivex.subjects.PublishSubject
import io.reactivex.{BackpressureStrategy, Flowable}
import org.json4s.JsonAST.JValue

object Sensors {

  case class Encoding(name: String)

  object Encodings {
    val PDF = Encoding("application/pdf")
    val SensorML = Encoding("http://www.opengis.net/doc/IS/SensorML/2.0")
    val GEOJson = Encoding("application/vnd.geo+json")

    def fromName(name: String): Encoding = name match {
      case "application/pdf" => PDF
      case "http://www.opengis.net/doc/IS/SensorML/2.0" => SensorML
      case "application/vnd.geo+json" => GEOJson
    }

  }

  case class ObservationType(name: String)

  object ObservationTypes {
    val OM_CategoryObservation =
      ObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation")
    val OM_CountObservation =
      ObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation")
    val OM_Measurement =
      ObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement")
    val OM_Observation =
      ObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation")
    val OM_TruthObservation =
      ObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation")

    def fromName(name: String): ObservationType = name match {
      case "OM_CategoryObservation" => OM_CategoryObservation
      case "OM_CountObservation" => OM_CountObservation
      case "OM_Measurement" => OM_Measurement
      case "OM_Observation" => OM_Observation
      case "OM_TruthObservation" => OM_TruthObservation
    }
  }

  case class Thing(
    name: String,
    description: String,
    location: Iterable[Location],
    historicalLocation: Iterable[HistoricalLocation],
    properties: Option[JValue] = None
  )

  case class Location(
    name: String,
    description: String,
    encodingType: Encoding,
    location: Any
  )

  case class HistoricalLocation(
    time: Long
  )

  case class UnitOfMeasurement(
    name: String,
    symbol: String,
    definition: URI
  )

  case class DataStreamCustomProps(
    name: Option[String],
    description: Option[String],
    featureOfInterest: Option[FeatureOfInterest])

  case class DataStreamMetadata(
    description: String,
    unitOfMeasurement: UnitOfMeasurement,
    featureOfInterest: FeatureOfInterest,
    observationType: ObservationType,
    observedProperty: ObservedProperty)

  class DataStream private (
    val name: String,
    val description: String,
    val unitOfMeasurement: UnitOfMeasurement,
    val featureOfInterest: FeatureOfInterest,
    val observationType: ObservationType,
    val observedProperty: ObservedProperty,
    val procedure: (DataStream) => Observation,
    val sensor: Device = null,
    val observedArea: Option[Any] = None,
    val phenomenonTime: Option[Long] = None,
    val resultTime: Option[Long] = None,
    obsSubject: Option[PublishSubject[Observation]] = None
  ) {

    protected implicit val self: DataStream = this
    protected def doObservationBinded()(implicit ds: DataStream) = procedure(ds)

    val doObservation: () => Observation = () =>  doObservationBinded()

    private val subj = obsSubject getOrElse PublishSubject.create[Observation]
    private var obsEmitter =
      Observations.atSampleRate(doObservation, 1000).subscribe(obs => subj.onNext(obs))

    lazy val observable: Flowable[Observation] = subj.toFlowable(BackpressureStrategy.LATEST)

    def updateWith(
      newName: String = name,
      newDescription: String = description,
      newFeatureOfInterest: FeatureOfInterest = featureOfInterest,
      newSensor: Device = sensor,
      newProcedure: (DataStream) => Observation = procedure
    ): DataStream = {
      obsEmitter.dispose()
      println(s"copied: newProcedure ${newProcedure(this).result}")
      new DataStream(
        newName, newDescription, unitOfMeasurement, newFeatureOfInterest, observationType,
        observedProperty, newProcedure, newSensor, observedArea, phenomenonTime, resultTime,
        Some(subj))
    }

  }
  object DataStream {
    def apply(
      name: String,
      description: String,
      unitOfMeasurement: UnitOfMeasurement,
      featureOfInterest: FeatureOfInterest,
      observationType: ObservationType,
      observedProperty: ObservedProperty,
      procedure: (DataStream) => Observation,
      sensor: Device = null,
      observedArea: Option[Any] = None,
      phenomenonTime: Option[Long] = None,
      resultTime: Option[Long] = None
    ): DataStream = new DataStream(
      name, description, unitOfMeasurement, featureOfInterest, observationType,
      observedProperty, procedure, sensor, observedArea, phenomenonTime, resultTime
    )
  }

  case class ObservedProperty(
    name: String,
    definition: URI,
    description: String
  )

  case class Observation(
    phenomenonTime: Long,
    resultTime: Long,
    result: Any,
    parentDataStream: DataStream,
    validTime: Option[Period] = None,
    parameters: Option[JValue] = None
  ) {
    override lazy val toString =
      s"[id:${parentDataStream.sensor.id}][$phenomenonTime] ~> $result"
  }

  case class FeatureOfInterest(
    name: String,
    description: String,
    encodingType: Encoding = Encodings.GEOJson,
    feature: Any
  )

}