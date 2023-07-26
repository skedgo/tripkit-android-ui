package com.technologies.tripkituisample

import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.tripresult.ActionButtonViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object AppEventBus {
    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class AppEvent {
    data class OnActionClicked(
        val tripActions: TripActions,
        val actionButtonViewModel: ActionButtonViewModel? = null,
        val trip: Trip? = null,
        val tripSegment: TripSegment? = null
    )

    data class ViewTripSelected(
        val viewTrip: ViewTrip,
        val tripGroupList: List<TripGroup>
    )

    data class ViewTripDetails(val tripSegment: TripSegment)
}