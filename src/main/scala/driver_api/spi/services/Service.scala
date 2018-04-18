package driver_api.spi.services

import rx.lang.scala.Observable
import st.api.SensorThings.Observation

trait Service {

  def init(obsBus: Observable[Observation]): Unit

  def dispose(): Unit

}
