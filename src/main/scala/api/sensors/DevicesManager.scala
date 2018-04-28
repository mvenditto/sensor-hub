package api.sensors

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

import api.devices.Devices.Device
import api.internal.DeviceDriverWrapper
import api.sensors.Sensors.{Encoding, Observation}
import api.services.security.permission.DriverManagementPermission
import macros.permission.GrantWith
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.{Observable, Subscription}

object DevicesManager {

  private val idFactory = new AtomicInteger(0)
  private def newId(): Int = idFactory.getAndIncrement()

  private var _sensors = Map.empty[Int, Device]
  private var _obsBusSubscriptions = Map.empty[Int, Seq[Subscription]]
  private val _obsBus = PublishSubject[Observation]()

  def obsBus: Observable[Observation] = _obsBus.asInstanceOf[Observable[Observation]]

  def sensors(): Iterable[Device] = _sensors.values

  def getSensor(id: Int): Option[Device] = _sensors.get(id)

  def deleteSensor(id: Int): Unit = {
    _sensors = _sensors.filter(_._1 != id)
    _obsBusSubscriptions.get(id).foreach(_.foreach(_.unsubscribe))
    _obsBusSubscriptions = _obsBusSubscriptions.filter(_._1 != id)
  }

  def createSensor(
    name: String, description: String,
    encodingType: Encoding, metadata: URI, driver: DeviceDriverWrapper): Device = {
    val sensor = Device(newId(), name, description, encodingType, metadata, driver)
    _sensors = _sensors ++ Map(sensor.id -> sensor)
    _obsBusSubscriptions = _obsBusSubscriptions ++ Map(sensor.id -> subscribeToObsBus(sensor))
    sensor
  }

  private def subscribeToObsBus(sensor: Device): Seq[Subscription] = {
    sensor.dataStreams.map(ds => ds.observable.subscribe(obs => _obsBus.onNext(obs))).toSeq
  }

}
