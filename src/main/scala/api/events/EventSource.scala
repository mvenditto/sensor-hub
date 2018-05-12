package api.events

import api.events.SensorsHubEvents.SensorsHubEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


trait EventSource {

  protected val eventBus = PublishSubject.create[SensorsHubEvent]()

  val events: Observable[SensorsHubEvent] = eventBus.asInstanceOf[Observable[SensorsHubEvent]]

  def trigger(e: SensorsHubEvent): Unit = eventBus.onNext(e)

}
