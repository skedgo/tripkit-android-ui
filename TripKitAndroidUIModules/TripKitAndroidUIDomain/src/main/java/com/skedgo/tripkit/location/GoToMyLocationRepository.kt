package com.skedgo.tripkit.location

import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.rxtry.Try
import com.skedgo.rxtry.toTry
import io.reactivex.Observable
import javax.inject.Inject

open class GoToMyLocationRepository @Inject internal constructor(
    private val userGeoPointRepository: UserGeoPointRepository
) {
    private val goToMyLocationSignal = PublishRelay.create<Unit>()

    open val myLocation: Observable<Try<GeoPoint>>
        get() = goToMyLocationSignal
            .hide()
            // To cancel previous retrieval.
            .switchMap { getCurrentLocation() }

    open fun goToMyLocation() = goToMyLocationSignal.accept(Unit)

    private fun getCurrentLocation(): Observable<Try<GeoPoint>> =
        userGeoPointRepository.getFirstCurrentGeoPoint()
            .toTry()
}
