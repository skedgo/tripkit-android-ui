package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.GCSkedgoResult
import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.common.model.LOCATION_CLASS_SCHOOL
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import com.skedgo.tripkit.ui.search.FetchTripGoLocations
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class FetchTripGoLocationsImpl @Inject internal constructor(
    private val geocoder: GeocoderLive,
    private val transportModeSharedPreference: TransportModeSharedPreference
) : FetchTripGoLocations {
    override fun getLocations(
        parameters: FetchLocationsParameters
    ): Observable<List<GCResultInterface>> =
        Observable
            .create(ObservableOnSubscribe<List<GCResultInterface>> { subscriber ->
                try {
                    geocoder.nearLatitude = parameters.nearbyLat()
                    geocoder.nearLongitude = parameters.nearbyLon()
                    val results = getLocationsFromTripGo(parameters.term())
                    subscriber.onNext(results)
                    subscriber.onComplete()
                } catch (e: Exception) {
                    Timber.e(e)
                }
            })
            /* Don't let any error of one source block one another source. */
            .onErrorResumeNext(Observable.empty())
            .subscribeOn(Schedulers.newThread())

    @Throws(IOException::class)
    fun getLocationsFromTripGo(term: String): List<GCResultInterface> {
        val suggestionsFromTripGo = geocoder.query(term) ?: emptyList()
        when {
            suggestionsFromTripGo.isNotEmpty() -> {
                val results = ArrayList<GCResultInterface>(suggestionsFromTripGo.size)
                for (location in suggestionsFromTripGo) {
                    if (location.w3w != null) {
                        location.locationType = Location.TYPE_W3W
                    } else if (location.locationClass == LOCATION_CLASS_SCHOOL) {
                        location.locationType = Location.TYPE_SCHOOL
                    }
                    location.source = Location.TRIPGO

                    if (location.locationType != Location.TYPE_SCHOOL ||
                        transportModeSharedPreference.isSchoolBusModeEnabled()
                    ) {
                        results.add(
                            SkedgoResultLocationAdapter(
                                location,
                                GCSkedgoResult(
                                    location.name,
                                    location.lat,
                                    location.lon,
                                    location.locationClass ?: "",
                                    location.popularity,
                                    location.modeIdentifiers
                                )
                            )
                        )
                    }
                }
                return results
            }

            else -> return emptyList()
        }
    }
}
