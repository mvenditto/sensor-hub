package api.events

import api.events.SensorsHubEvents.SensorsHubEvent
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject

trait EventSource {

  protected val eventBus = PublishSubject[SensorsHubEvent]()

  val events: Observable[SensorsHubEvent] = eventBus.asInstanceOf[Observable[SensorsHubEvent]]

  def trigger(e: SensorsHubEvent): Unit = eventBus.onNext(e)

}
