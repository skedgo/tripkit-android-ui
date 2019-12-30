package com.skedgo.tripkit.ui.tripprogress

import com.skedgo.rxtry.Try
import com.skedgo.tripkit.location.LocationSample
import com.skedgo.tripkit.location.UserGeoPointRepository
import com.skedgo.tripkit.ui.personaldata.MyPersonalDataRepository
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import com.skedgo.tripkit.routing.Trip
import javax.inject.Inject

open class UpdateTripProgressWithUserLocation @Inject internal constructor(
        private val userGeoPointRepository: UserGeoPointRepository,
        private val updateTripProgress: UpdateTripProgress,
        private val myPersonalDataRepository: MyPersonalDataRepository
) {
    open fun execute(tripStream: Observable<Trip>): Observable<Try<Unit>> =
            myPersonalDataRepository.isUploadTripProgressEnabled()
                    .flatMapObservable {
                        if (it) {
                            userGeoPointRepository.getPeriodicLocationSample(intervalSecs = 10)
                                    .withLatestFrom(tripStream.filter { it.progressURL != null },
                                            BiFunction<LocationSample, Trip, Pair<String, List<LocationSample>>> { locationSample, trip ->
                                                trip.progressURL!! to listOf(locationSample)
                                            })
                                    .flatMap { (progressURL, locationSamples) ->
                                        updateTripProgress.execute(progressURL, locationSamples)
                                    }
                        } else {
                            Observable.empty<Try<Unit>>()
                        }
                    }
}

