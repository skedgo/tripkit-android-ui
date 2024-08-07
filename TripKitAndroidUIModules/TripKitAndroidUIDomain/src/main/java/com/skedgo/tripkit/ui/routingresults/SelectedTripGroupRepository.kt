package com.skedgo.tripkit.ui.routingresults

import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.routing.TripGroup
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

open class SelectedTripGroupRepository constructor(
    private val tripGroupRepository: TripGroupRepository
) {
    private val selectedTripGroupId = BehaviorRelay
        .create<String>()
        .toSerialized()

    private val selectedTripGroup = selectedTripGroupId
        .distinctUntilChanged()
        .switchMap {
            tripGroupRepository.getTripGroup(it)
        }
        .observeOn(Schedulers.computation())

    open fun getSelectedTripGroup(): Observable<TripGroup> {
        return selectedTripGroup
    }

    open fun setSelectedTripGroupId(selectedTripGroupId: String) {
        this.selectedTripGroupId.accept(selectedTripGroupId)
    }
}
