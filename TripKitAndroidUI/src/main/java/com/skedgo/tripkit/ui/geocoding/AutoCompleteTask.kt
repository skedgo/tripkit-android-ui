package com.skedgo.tripkit.ui.geocoding

import android.util.Log
import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.data.connectivity.ConnectivityService
import com.skedgo.tripkit.ui.search.*
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

open class AutoCompleteTask @Inject internal constructor(
        private val filterSupportedLocations: FilterSupportedLocations,
        private val fetchLocalLocationsProvider: Provider<FetchLocalLocations>,
        private val fetchFoursquareLocationsProvider: Provider<FetchFoursquareLocations>,
        private val fetchGoogleLocationsProvider: Provider<FetchGoogleLocations>,
        private val fetchTripGoLocationsProvider: Provider<FetchTripGoLocations>,
        private val connectivityService: ConnectivityService,
        private val resultAggregator: ResultAggregator
) : FetchSuggestions {
  private fun queryLocations(
          filterSupportedLocations: FilterSupportedLocations,
          fetchLocationParameters: FetchLocationsParameters,
          fetchLocalLocations: FetchLocalLocations,
          fetchGoogleLocations: FetchGoogleLocations,
          fetchFoursquareLocations: FetchFoursquareLocations,
          fetchTripGoLocations: FetchTripGoLocations
  ): Observable<List<GCResultInterface>> =
      when {
        fetchLocationParameters.term().isNullOrBlank() ->
          fetchLocalLocations.getLocations(fetchLocationParameters)
        else -> Observable
            .merge(Observable.empty(),
                fetchLocalLocations.getLocations(fetchLocationParameters),
                fetchGoogleLocations.getLocations(fetchLocationParameters)
                    .onErrorResumeNext(
                        fetchFoursquareLocations
                            .getLocations(fetchLocationParameters)
                            .flatMap(filterSupportedLocations::invoke)
                    ),
                fetchTripGoLocations.getLocations(fetchLocationParameters)
            )
      }

  override fun query(parameters: FetchLocationsParameters): Observable<AutoCompleteResult> {
    return if (!connectivityService.isNetworkConnected) {
        Timber.e("Network is not connected")
      if (parameters.term().orEmpty().isEmpty()) {
        Observable.just<AutoCompleteResult>(HasResults(emptyList()))
      } else {
        Observable.just<AutoCompleteResult>(NoConnection)
      }
    } else {
      queryLocations(
          filterSupportedLocations,
          parameters,
          fetchLocalLocationsProvider.get(),
          fetchGoogleLocationsProvider.get(),
          fetchFoursquareLocationsProvider.get(),
          fetchTripGoLocationsProvider.get())
          .doOnError { Timber.e("Error", it) }
          .scan(emptyList<List<GCResultInterface>>()) { x, y -> x.plusElement(y) }
          .map { resultAggregator.aggregate(parameters, it) }
          .filter { it.isNotEmpty() }
          .map { HasResults(it) as AutoCompleteResult }
          .switchIfEmpty(
              Observable.defer {
                if (parameters.term().isNullOrEmpty()) {
                  Observable.just<AutoCompleteResult>(HasResults(emptyList()))
                } else {
                  Observable.just<AutoCompleteResult>(NoResult(parameters.term()))
                }
              }
          )
    }
  }
}