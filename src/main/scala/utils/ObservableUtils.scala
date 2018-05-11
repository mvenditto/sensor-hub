package utils

import io.reactivex.functions.Cancellable
import io.reactivex.{BackpressureStrategy, Flowable, FlowableEmitter, Observable}


object ObservableUtils {

  def flowableFromFunc[T](func: () => T): Flowable[T] = {
    Flowable.create((emitter: FlowableEmitter[T]) => {
      new Thread(() => {
        var shouldEmit = true
        emitter.setCancellable(() => shouldEmit = false)
        while (shouldEmit) if(!emitter.isCancelled) emitter.onNext(func())

      }).start()
    }, BackpressureStrategy.LATEST)
  }

  /*
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
  }*/
}
