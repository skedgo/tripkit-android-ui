package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.data.regions.RegionService
import io.reactivex.Observable
import javax.inject.Inject

class FilterSupportedLocationsImpl @Inject internal constructor(
    val regionService: RegionService
) : FilterSupportedLocations {
    override operator fun invoke(
        results: List<GCResultInterface>
    ): Observable<List<GCResultInterface>> = Observable.empty()
//  ): Observable<List<GCResultInterface>> =
//      Observable.fromIterable(results)
//          .filter { result ->
//            regionService
//                .getRegionByLocationAsync(result.lat, result.lng)
//                .onErrorResumeNext { error ->
//                  when (error) {
//                    is OutOfRegionsException -> Observable.empty()
//                    else -> Observable.error(error)
//                  }
//                }
//                .isEmpty
//                .map { isEmpty -> !isEmpty }
//                .toBlocking()
//                .first()
//          }
//          .toList()
}
