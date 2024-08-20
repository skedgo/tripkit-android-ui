package com.skedgo.tripkit.ui.routingresults

import com.skedgo.tripkit.analytics.TripSource
import com.skedgo.tripkit.ui.tracking.Event
import com.skedgo.tripkit.ui.tracking.EventTracker
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class TrackViewingTrip @Inject internal constructor(
    private val eventTracker: EventTracker
) {

    open fun execute(getSource: Observable<TripSource>): Observable<Unit> =
        getSource.debounce(5, TimeUnit.SECONDS)
            .flatMap {
                Observable.fromCallable {
                    eventTracker.log(Event.ViewTrip(it.value))
                }
            }
}