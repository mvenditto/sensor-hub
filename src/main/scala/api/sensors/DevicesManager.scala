package api.sensors

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

import api.devices.Devices.Device
import api.events.EventBus
import api.events.SensorsHubEvents.{DeviceCreated, DeviceDeleted}
import api.internal.{DeviceDriverWrapper, DisposableManager}
import api.sensors.Sensors.{DataStream, DataStreamCustomProps, Encoding, Observation}
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

import scala.collection.concurrent.TrieMap


object DevicesManager{

  private val idFactory = new AtomicInteger(0)
  private def newId(): Int = idFactory.getAndIncrement()

  private var _devices = TrieMap.empty[Int, Device]
  private var _obsBusSubscriptions = TrieMap.empty[Int, Seq[Disposable]]
  private val _obsBus = PublishSubject.create[Observation]()

  def obsBus: Observable[Observation] = _obsBus.asInstanceOf[Observable[Observation]]

  def devices(): Iterable[Device] = _devices.values

  def getDevice(id: Int): Option[Device] = _devices.get(id)

  def deleteDevice(id: Int): Unit = {
    _devices.find(_._1 == id).foreach(dev => {
      _devices = _devices.filter(_._1 != id)
      _obsBusSubscriptions.get(id).foreach(_.foreach(_.dispose()))
      _obsBusSubscriptions = _obsBusSubscriptions.filter(_._1 != id)
      EventBus.trigger(DeviceDeleted(dev._2))
    })
  }

  def createDevice(
    name: String, description: String,
    encodingType: Encoding, metadata: URI, driver: DeviceDriverWrapper,
      dsMap: Map[String, DataStreamCustomProps] = Map.empty): Device = {
    val sensor = Device(newId(), name, description, encodingType, metadata, driver, dsMap)
    val disposables = DisposableManager.addAll(subscribeToObsBus(sensor))
    _devices.put(sensor.id, sensor)
    _obsBusSubscriptions.put(sensor.id, disposables)
    EventBus.trigger(DeviceCreated(sensor))
    sensor
  }

  private def subscribeToObsBus(sensor: Device): Seq[Disposable] = {
    sensor.dataStreams.map(ds => ds.observable.subscribe(obs => _obsBus.onNext(obs))).toSeq
  }

}
