package api.internal

import java.util.concurrent.{Executors, TimeUnit}

import api.sensors.Sensors.Observation
import io.reactivex.disposables.Disposable
import io.reactivex.{BackpressureStrategy, Flowable, FlowableEmitter}


object Observations {

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
    }, BackpressureStrategy.LATEST)

}
