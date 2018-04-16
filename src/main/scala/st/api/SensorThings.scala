package st.api

import java.net.URI
import java.time.{Instant, Period}

import org.json4s.JsonAST.JValue
import rx.lang.scala.Observable
import utils.ObservableUtils

object SensorThings {

  case class Encoding(name: String)

  object Encodings {
    val PDF = Encoding("application/pdf")
    val SensorML = Encoding("http://www.opengis.net/doc/IS/SensorML/2.0")
    val GEOJson = Encoding("application/vnd.geo+json")
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
    time: Instant
  )

  case class UnitOfMeasurement(
    name: String,
    symbol: String,
    definition: URI
  )

  case class DataStream(
    name: String,
    description: String,
    unitOfMeasurement: UnitOfMeasurement,
    observationType: ObservationType,
    observedProperty: ObservedProperty,
    procedure: () => Observation,
    sensor: Sensor = null,
    observedArea: Option[Any] = None,
    phenomenonTime: Option[Instant] = None,
    resultTime: Option[Instant] = None
  ) {
    val observable: Observable[Observation] =
      ObservableUtils.observableFromFunc(procedure)
  }

  case class Sensor(
    name: String,
    description: String,
    dataStreams: Iterable[DataStream],
    encodingType: Encoding,
    metadata: URI
  )

  case class ObservedProperty(
    name: String,
    definition: URI,
    description: String
  )

  case class Observation(
    phenomenonTime: Instant,
    resultTime: Instant,
    result: Any,
    featureOfInterest: FeatureOfInterest,
    //resultQuality: Iterable[]
    validTime: Option[Period] = None,
    parameters: Option[JValue] = None
  ) {
    override lazy val toString =
      s"[$phenomenonTime] ~> $result"
  }

  case class FeatureOfInterest(
    name: String,
    description: String,
    encodingType: Encoding = Encodings.GEOJson,
    feature: Any
  )

}