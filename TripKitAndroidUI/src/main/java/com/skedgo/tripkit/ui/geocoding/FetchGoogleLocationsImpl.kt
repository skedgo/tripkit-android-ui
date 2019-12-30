package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.GCGoogleResult
import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.search.FetchGoogleLocations
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class FetchGoogleLocationsImpl @Inject constructor(
    placeSearchRepository: PlaceSearchRepository
) : FetchGoogleLocations {
  private val googleGeocoder: GoogleGeocoderLive = GoogleGeocoderLive(placeSearchRepository)
  private val maxResult = 20

  override fun getLocations(parameters: FetchLocationsParameters): Observable<List<GCResultInterface>> {
    return getLocationFromGoogleClient(parameters)
        .subscribeOn(Schedulers.newThread())
  }

  private fun getLocationFromGoogleClient(
      parameters: FetchLocationsParameters
  ): Observable<List<GCResultInterface>> =
      googleGeocoder.query(
          parameters.term(),
          maxResult,
          parameters.southwestLat(),
          parameters.southwestLon(),
          parameters.northeastLat(),
          parameters.northeastLon())
          .toList()
          .map { aggregateGoogleResults(it) }.toObservable()

  private fun aggregateGoogleResults(
      locations: List<Place.WithoutLocation>
  ): List<GCResultInterface> =
      locations
          .map {
            GoogleResultLocationAdapter(
                it,
                GCGoogleResult(it.prediction.primaryText, -1.0, -1.0, "")
            )
          }
}
