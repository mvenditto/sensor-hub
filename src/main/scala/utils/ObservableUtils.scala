package utils

import rx.lang.scala.Observable

object ObservableUtils {

  def observableFromFunc[T](func: () => T): Observable[T] = {
      Observable[T] { sub => {
        new Thread(() => {
          while (true) {
            if (!sub.isUnsubscribed)
              sub.onNext(func())
          }
          sub.onCompleted()
        }).start()
      }
    }.onBackpressureLatest
  }
}
