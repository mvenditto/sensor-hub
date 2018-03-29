package driver_api

import driver_api.sensor.SensingApi.{Observation, PropertyObserver}
import rx.lang.scala.Observable
import utils.ObservableUtils

trait ObservablesSupport extends DeviceController {

  lazy val propertyStreams: Map[String, Observable[Observation]] = (for {
    po <- propertyObservers
  } yield
      po.observedProperty.name -> ObservableUtils.observableFromFunc(po.procedure)
    ).toMap
}


trait DeviceController {

  val configurator: DeviceConfiguration

  var propertyObservers = Seq.empty[PropertyObserver]

  def init(): Unit

  def start(): Unit

  def stop(): Unit

  protected implicit class PORegistrar(op: PropertyObserver) {
    println(s"register:$op")
    def register(): Unit = propertyObservers  :+= op
  }
}