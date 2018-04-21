package spi.service

import api.sensors.Sensors.Observation
import rx.lang.scala.Observable

trait Service extends Disposable {

  def init(obsBus: Observable[Observation]): Unit

  def restart(): Unit

}
