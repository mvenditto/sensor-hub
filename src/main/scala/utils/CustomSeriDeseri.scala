package utils

import java.net.URI
import java.time.Instant

import api.events.SensorsHubEvents.SensorsHubEvent
import api.sensors.DevicesManager
import api.sensors.Sensors._
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.compact
import org.json4s.{CustomSerializer, Extraction}

object CustomSeriDeseri {

  case class DeviceMetadataWithId(
    id: Int,
    name: String,
    description: String,
    metadataEncoding: String,
    metadata: String,
    driverName: String
  )

  implicit val fmt = org.json4s.DefaultFormats ++ Traversable(
      new DeviceMetadataSerializer(),
      new URISerializer(),
      new ObservedPropertySerializer(),
      new FeatureOfInterestSerializer(),
      new DataStreamSerializer(),
      new UnitOfMeasurementSerializer())


  class URISerializer extends CustomSerializer[URI](_ => ( {
    case json: JObject => new URI(json.toString)
  }, {
    case uri: URI => uri.toString
  }
  ))

  class DeviceMetadataSerializer extends CustomSerializer[DeviceMetadataWithId](_ => ({
      case json: JObject =>
        DeviceMetadataWithId(
          id = (json \ "id").extract[Int],
          name = (json \ "name").extract[String],
          description = (json \ "description").extract[String],
          metadataEncoding = (json \ "metadataEncoding").extract[String],
          metadata = (json \ "metadata").extract[String],
          driverName = (json \ "driverName").extract[String]
        )
    }, {
      case dev: DeviceMetadataWithId =>
        JObject(
          JField("id", JInt(dev.id)) ::
            JField("name", JString(dev.name)) ::
            JField("description", JString(dev.description)) ::
            JField("metadataEncoding", JString(dev.metadataEncoding)) ::
            JField("metadata", JString(dev.metadata)) ::
            JField("driverName", JString(dev.driverName))
            :: Nil)
    }
  ))

  class FeatureOfInterestSerializer extends CustomSerializer[FeatureOfInterest](_ => ( {
    case json: JObject =>
      FeatureOfInterest(
        (json \ "name").extract[String],
        (json \ "description").extract[String],
        Encodings.fromName((json \ "encoding").extract[String]),
        json \ "feature")
  }, {
    case fov: FeatureOfInterest =>
      JObject(
          JField("name", JString(fov.name)) ::
          JField("description", JString(fov.description)) ::
          JField("encoding", JString(fov.encodingType.name)) ::
          JField("feature", Extraction.decompose(fov.feature)) :: Nil)
  }
  ))

  class ObservedPropertySerializer extends CustomSerializer[ObservedProperty](_ => ( {
    case json: JObject =>
      ObservedProperty(
        (json \ "name").extract[String],
        new URI((json \ "definition").extract[String]),
        (json \ "description").extract[String])
  }, {
    case op: ObservedProperty =>
      JObject(
        JField("name", JString(op.name)) ::
          JField("definition", JString(op.definition.toString)) ::
          JField("description", JString(op.description)) :: Nil)
  }
  ))

  class UnitOfMeasurementSerializer extends CustomSerializer[UnitOfMeasurement](_ => ( {
    case json: JObject =>
      UnitOfMeasurement(
        (json \ "name").extract[String],
        (json \ "symbol").extract[String],
        new URI((json \ "definition").extract[String]))
  }, {
    case op: UnitOfMeasurement =>
      JObject(
        JField("name", JString(op.name)) ::
          JField("definition", JString(op.definition.toString)) ::
          JField("symbol", JString(op.symbol)) :: Nil)
  }))

  class DataStreamSerializer extends CustomSerializer[DataStream](_ => ( {
    case json: JObject =>
      DataStream(
        name = (json \ "name").extract[String],
        description = (json \ "description").extract[String],
        featureOfInterest = (json \ "featureOfInterest").extract[FeatureOfInterest],
        observationType = ObservationType((json \ "observationType").extract[String]),
        observedProperty = (json \ "observedProperty").extract[ObservedProperty],
        sensor = DevicesManager.getDevice((json \ "sensor_id").extract[Int]).orNull, //TODO not working, needs fix!
        procedure = (ds: DataStream) => null,
        unitOfMeasurement = (json \ "unitOfMeasurement").extract[UnitOfMeasurement])
  }, {
    case ds: DataStream =>
      JObject(
          JField("name", JString(ds.name)) ::
          JField("description", JString(ds.description)) ::
          JField("featureOfInterest", Extraction.decompose(ds.featureOfInterest)) ::
          JField("observationType", JString(ds.observationType.name)) ::
          JField("observedProperty", Extraction.decompose(ds.observedProperty)) ::
          JField("unitOfMeasurement", Extraction.decompose(ds.unitOfMeasurement)) ::
          JField("sensor", JObject(
            JField("id", JInt(ds.sensor.id)) ::
            JField("name", JString(ds.sensor.name)) ::
            JField("driver", Extraction.decompose(ds.sensor.driver.metadata)) ::
            JField("description", JString(ds.sensor.description)) :: Nil)) :: Nil)
  }))

  def evtToJson(evt: SensorsHubEvent): String = compact(
    JObject(
      JField("name", JString(evt.getClass.getName)) ::
      JField("timeStamp", JLong(evt.timestamp)) ::
      JField("event", Extraction.decompose(evt)) ::
      Nil
    ))

}