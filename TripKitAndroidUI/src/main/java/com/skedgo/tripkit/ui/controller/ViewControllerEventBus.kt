package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.controller.utils.LocationField
import com.skedgo.tripkit.ui.locationpointer.LocationPointerFragment
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
    data class OnChooseOnMap(val locationField: LocationField)
    data class OnLocationChosen(val location: Location, val locationField: LocationField)
    data class OnLocationSuggestionSelected(val suggestion: Any)
    data class OnCitySelected(val location: Location)
    data class OnLocationSelected(val location: Location)
    data class OnGetRouteTripResults(val origin: Location, val destination: Location)
    data class OnShareTrip(val trip: Trip)
    data class OnLaunchReportingTripBug(val trip: Trip)
    data class OnTripPrimaryActionClick(val tripSegment: TripSegment, val fromListOverviewAction: Boolean)
    data class OnViewTrip(val viewTrip: ViewTrip, val tripGroupList: List<TripGroup>)

    data class OnShowRouteSelection(val startLocation: Location, val destLocation: Location)
}