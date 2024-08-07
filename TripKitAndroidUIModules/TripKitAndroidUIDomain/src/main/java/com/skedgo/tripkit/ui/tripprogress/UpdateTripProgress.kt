package com.skedgo.tripkit.ui.tripprogress

import com.skedgo.rxtry.Try
import com.skedgo.tripkit.location.LocationSample
import io.reactivex.Observable

interface UpdateTripProgress {
    fun execute(progressUrl: String, samples: List<LocationSample>): Observable<Try<Unit>>
}
