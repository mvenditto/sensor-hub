package api.internal

import java.util.concurrent.{Executor, ExecutorService, Executors, TimeUnit}

import api.devices.Sensors.Observation
import io.reactivex.disposables.Disposable
import io.reactivex.{BackpressureStrategy, Flowable, FlowableEmitter}


object Observations {

  val disposable: (ExecutorService) => Disposable = (e: ExecutorService) => {
    new Disposable {
      override def isDisposed: Boolean = e.isTerminated
      override def dispose(): Unit = e.shutdownNow()
    }
  }

  def atSampleRate(doObs: () => Observation, sampleRate: Long, initDelay: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS): Flowable[Observation] =
    Flowable.create((emitter: FlowableEmitter[Observation]) => {
      val exec = Executors.newSingleThreadScheduledExecutor()
      exec.scheduleAtFixedRate(() => {
        try {
          emitter.onNext(doObs())
        } catch {
          case t: Throwable =>  emitter.onError(t)
        }
      }, initDelay, sampleRate, unit)
      val disposableExec = disposable(exec)
      emitter.setDisposable(disposableExec)
      DisposableManager.add(disposableExec)
    }, BackpressureStrategy.LATEST)
}
