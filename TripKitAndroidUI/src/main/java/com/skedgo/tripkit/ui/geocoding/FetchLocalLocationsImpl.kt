package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.search.FetchLocalLocations
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import io.reactivex.Observable
import javax.inject.Inject

class FetchLocalLocationsImpl @Inject constructor(
//        private val favoriteStore: FavoriteStore,
    private val errorLogger: ErrorLogger
//        private val schedulerFactory: SchedulerFactory
) : FetchLocalLocations {
    override fun getLocations(parameters: FetchLocationsParameters): Observable<List<GCResultInterface>> {
        return Observable.empty() // TODO
//    return favoriteStore
//        .getFavoritesByTermAsync(parameters.term())
//        .map { favorite -> favorite.toLocation() }
//        .filter { location -> Location.isValidLocation(location) }
//        .map { checkNotNull(it) }
//        .map<GCResultInterface> { location ->
//          location.isFavourite(true)
//          location.source = Location.LOCAL
//          AppResultLocationAdapter(
//              location,
//              GCAppResult(
//                  // Some favorites can have nullable name.
//                  if (location.name != null) location.name else "",
//                  location.lat,
//                  location.lon,
//                  // Some stops may have null address.
//                  if (location.address != null) location.address else "",
//                  location.isFavourite,
//                  GCAppResultInterface.Source.History))
//        }
//        .toList()
//        /* For debugging purposes only. */
//        .doOnError { errorLogger.logError(it) }
//        /* Don't let any error of one source block one another source. */
//        .onErrorResumeNext(Observable.empty())
//        .subscribeOn(io())
    }
}
