package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.search.FetchFoursquareLocations
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import javax.inject.Inject

class FetchFoursquareLocationsImpl @Inject internal constructor(
    private val schedulerFactory: SchedulerFactory
) : FetchFoursquareLocations {
    override fun getLocations(
        parameters: FetchLocationsParameters
    ): Observable<List<GCResultInterface>> =
        Observable
            .create(ObservableOnSubscribe<List<GCResultInterface>> { subscriber ->
                val suggestionsFromFoursquare = getLocationFromFoursquareClient(parameters)
                subscriber.onNext(suggestionsFromFoursquare)
                subscriber.onComplete()
            })
            /* Don't let any error of one source terminate one another source. */
            .onErrorResumeNext(Observable.empty())
            .subscribeOn(schedulerFactory.ioScheduler)

    private fun getLocationFromFoursquareClient(
        parameters: FetchLocationsParameters
    ): List<GCResultInterface> {
        val foursquareGeocoder = FoursquareGeocoder(
            parameters.term(),
            parameters.nearbyLat(),
            parameters.nearbyLon()
        )
        return ArrayList<GCResultInterface>(foursquareGeocoder.fromFoursquare)
    }
}
