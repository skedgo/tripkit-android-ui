package com.skedgo.tripkit.ui.routingresults

import com.skedgo.tripkit.routing.Trip
import io.reactivex.Observable
import javax.inject.Inject

open class GetSelectedTrip @Inject internal constructor(
    private val selectedTripGroupRepository: SelectedTripGroupRepository
) {
    open fun execute(): Observable<Trip> {
        return selectedTripGroupRepository.getSelectedTripGroup()
            .map {
                it.displayTrip!!
            }
    }
}
