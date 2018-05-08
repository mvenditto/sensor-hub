package api.sensors

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

import api.devices.Devices.Device
import api.events.EventBus
import api.events.SensorsHubEvents.{DeviceCreated, DeviceDeleted}
import api.internal.DeviceDriverWrapper
import api.sensors.Sensors.{Encoding, Observation}
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.{Observable, Subscription}



object DevicesManager{

  private val idFactory = new AtomicInteger(0)
  private def newId(): Int = idFactory.getAndIncrement()

  private var _devices = Map.empty[Int, Device]
  private var _obsBusSubscriptions = Map.empty[Int, Seq[Subscription]]
  private val _obsBus = PublishSubject[Observation]()

  def obsBus: Observable[Observation] = _obsBus.asInstanceOf[Observable[Observation]]

  def devices(): Iterable[Device] = _devices.values

  def getDevice(id: Int): Option[Device] = _devices.get(id)

  def deleteDevice(id: Int): Unit = {
    _devices.find(_._1 == id).foreach(dev => {
      _devices = _devices.filter(_._1 != id)
      _obsBusSubscriptions.get(id).foreach(_.foreach(_.unsubscribe))
      _obsBusSubscriptions = _obsBusSubscriptions.filter(_._1 != id)
      EventBus.trigger(DeviceDeleted(dev._2))
    })
  }

  def createDevice(
    name: String, description: String,
    encodingType: Encoding, metadata: URI, driver: DeviceDriverWrapper): Device = {
    val sensor = Device(newId(), name, description, encodingType, metadata, driver)
    _devices = _devices ++ Map(sensor.id -> sensor)
    _obsBusSubscriptions = _obsBusSubscriptions ++ Map(sensor.id -> subscribeToObsBus(sensor))
    EventBus.trigger(DeviceCreated(sensor))
    sensor
  }

  private def subscribeToObsBus(sensor: Device): Seq[Subscription] = {
    sensor.dataStreams.map(ds => ds.observable.subscribe(obs => _obsBus.onNext(obs))).toSeq
  }

}
