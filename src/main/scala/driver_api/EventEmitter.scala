package driver_api

import io.reactivex.Observable

trait EventEmitter {

  val emitters: Map[String, Observable[String]]

}
