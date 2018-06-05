package api.devices

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

import api.devices.Devices.Device
import api.devices.Devices.Device.{mapDataStreams, mapTasks}
import api.devices.DriversHotSwap.UpdatableDevice
import api.devices.Sensors.{DataStreamCustomProps, Encoding, Observation}
import api.events.EventBus
import api.events.SensorsHubEvents.{DeviceCreated, DeviceDeleted, DriverChanged}
import api.internal.{DeviceDriverWrapper, DisposableManager, DriversManager}
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

import scala.collection.concurrent.TrieMap


object DevicesManager{

  private val idFactory = new AtomicInteger(0)
  private def newId(): Int = idFactory.getAndIncrement()

  private var _devices = TrieMap.empty[Int, Device with UpdatableDevice]
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
    //val sensor = Device(newId(), name, description, encodingType, metadata, driver, dsMap)

    val dev = new Device(newId(), name, description, encodingType, metadata,
      driver, mapTasks(driver), mapDataStreams(dsMap, driver), dsMap) with UpdatableDevice

    val disposables = DisposableManager.addAll(subscribeToObsBus(dev))
    _devices.put(dev.id, dev)
    _obsBusSubscriptions.put(dev.id, disposables)
    EventBus.trigger(DeviceCreated(dev))
    dev
  }

  private def subscribeToObsBus(sensor: Device): Seq[Disposable] = {
    sensor.dataStreams.map(ds => ds.observable.subscribe(obs => _obsBus.onNext(obs))).toSeq
  }

  EventBus.events.subscribe(evt => evt match {
    case DriverChanged(metadata) =>
      val devToUpdate = _devices.filter(_._2.driver.metadata.name equals metadata.name)
      DriversManager.instanceDriver(metadata.name)
        .map(DriversManager.initAndStart)
        .foreach(driver => devToUpdate.foreach(dev => {
          println(driver.metadata.name, "->", dev._1)
          val updatedDev = dev._2.updateWith(newDriver = driver)
          _devices.update(dev._2.id, updatedDev)
        }))
    case _ => ()
  })

}
