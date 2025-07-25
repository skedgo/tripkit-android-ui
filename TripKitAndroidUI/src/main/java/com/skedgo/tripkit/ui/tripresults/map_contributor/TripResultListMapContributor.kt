package com.skedgo.tripkit.ui.tripresults.map_contributor

import android.content.Context
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.tripresults.TripResultViewModel
import io.reactivex.subjects.BehaviorSubject

interface TripResultListMapContributor: TripKitMapContributor {
    fun setup(context: Context)
    fun setOriginDestinationLocations(from: Location?, to: Location?)
    fun setTripResultStreamObserver(
        context: Context,
        tripResultStream: BehaviorSubject<List<TripResultViewModel>>
    )
}