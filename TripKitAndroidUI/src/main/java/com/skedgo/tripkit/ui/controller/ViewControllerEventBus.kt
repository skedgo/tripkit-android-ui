package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.common.model.Location
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object ViewControllerEventBus {
    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class ViewControllerEvent {

    class OnCloseAction()
    data class OnLocationSuggestionSelected(val suggestion: Any)
    data class OnCitySelected(val location: Location)
    data class OnLocationSelected(val location: Location)
}