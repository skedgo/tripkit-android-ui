package com.skedgo.tripkit.ui.map

import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.tripplanner.NonCurrentType
import com.skedgo.tripkit.tripplanner.PinUpdate
import com.skedgo.tripkit.tripplanner.PinUpdateRepository
import com.skedgo.tripkit.ui.core.filterSome
import com.skedgo.tripkit.utils.OptionalCompat
import io.reactivex.Observable
import javax.inject.Inject

class PinUpdateRepositoryImpl @Inject internal constructor() : PinUpdateRepository {
    private val destination = BehaviorRelay.create<OptionalCompat<NonCurrentType>>()
    private val origin = BehaviorRelay.create<OptionalCompat<NonCurrentType>>()

    override fun getDestinationPinUpdate(): Observable<PinUpdate> =
        destination.hide()
            .filterSome()
            .switchMap {
                Observable.just(it)
                    .filter { it !is NonCurrentType.Stop }
                    .map<PinUpdate> { PinUpdate.Create(it) }
                    .startWith(PinUpdate.Delete)
            }

    override fun setDestinationPinUpdate(type: OptionalCompat<NonCurrentType>) {
        destination.accept(type)
    }

    override fun selectDestination() {
        destination.value?.let {
            destination.accept(it)
        }
    }

    override fun getOriginPinUpdate(): Observable<PinUpdate> =
        origin.hide()
            .filterSome()
            .switchMap {
                Observable.just(it)
                    .filter { it !is NonCurrentType.Stop }
                    .map<PinUpdate> { PinUpdate.Create(it) }
                    .startWith(PinUpdate.Delete)
            }

    override fun setOriginPinUpdate(type: OptionalCompat<NonCurrentType>) {
        origin.accept(type)
    }

    override fun selectOrigin() {
        origin.value?.let {
            origin.accept(it)
        }
    }
}
