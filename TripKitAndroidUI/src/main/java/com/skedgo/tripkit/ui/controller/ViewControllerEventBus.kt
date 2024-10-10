package com.skedgo.tripkit.ui.controller

import com.skedgo.geocoding.LatLng
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.controller.utils.LocationField
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
    data class OnViewPoiDetails(val location: Location)
    data class OnGetRouteTripResults(val origin: Location, val destination: Location)
    data class OnShareTrip(val trip: Trip)
    data class OnLaunchReportingTripBug(val trip: Trip)
    data class OnTripPrimaryActionClick(
        val tripSegment: TripSegment,
        val fromListOverviewAction: Boolean
    )

    data class OnViewTrip(val viewTrip: ViewTrip, val tripGroupList: List<TripGroup>)
    data class OnShowRouteSelection(val startLocation: Location, val destLocation: Location)
    data class OnRouteFromCurrentLocation(val location: Location)
    data class OnReportPlannedTrip(val tripGroups: List<TripGroup>, val trip: Trip)
    data class OnTripSegmentClicked(val tripSegment: TripSegment)
    data class OnBottomSheetFragmentCountUpdate(val count: Int)
    data class OnTripSegmentDataSetChange(val trip: Trip, val tripSegment: TripSegment)
    data class OnZoomToLocation(val latLng: LatLng)
    data class OnUpdateBottomSheetState(val state: Int)
}