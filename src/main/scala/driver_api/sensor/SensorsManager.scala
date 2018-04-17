package driver_api.sensor

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

import driver_api.DeviceDriverWrapper
import rx.lang.scala.{Observable, Subscription}
import rx.lang.scala.subjects.PublishSubject
import st.api.SensorThings.{Encoding, Observation, Sensor}

object SensorsManager {

  private val idFactory = new AtomicInteger(0)
  private def newId(): Int = idFactory.getAndIncrement()

  private var _sensors = Map.empty[Int, Sensor]
  private var _obsBusSubscriptions = Map.empty[Int, Seq[Subscription]]
  private val _obsBus = PublishSubject[Observation]()

  def obsBus: Observable[Observation] = _obsBus.asInstanceOf[Observable[Observation]]

  def sensors(): Iterable[Sensor] = _sensors.values

  def getSensor(id: Int): Option[Sensor] = _sensors.get(id)

  def deleteSensor(id: Int): Unit = {
    _sensors = _sensors.filter(_._1 != id)
    _obsBusSubscriptions.get(id).foreach(_.foreach(_.unsubscribe))
    _obsBusSubscriptions = _obsBusSubscriptions.filter(_._1 != id)
  }

  def createSensor(
    name: String, description: String,
    encodingType: Encoding, metadata: URI, driver: DeviceDriverWrapper): Sensor = {
    val sensor = Sensor(newId(), name, description, encodingType, metadata, driver)
    _sensors = _sensors ++ Map(sensor.id -> sensor)
    _obsBusSubscriptions = _obsBusSubscriptions ++ Map(sensor.id -> subscribeToObsBus(sensor))
    sensor
  }

  private def subscribeToObsBus(sensor: Sensor): Seq[Subscription] = {
    sensor.dataStreams.map(ds => ds.observable.subscribe(obs => _obsBus.onNext(obs))).toSeq
  }

}
