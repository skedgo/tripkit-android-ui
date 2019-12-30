package com.skedgo.tripkit.ui.search

import com.skedgo.geocoding.agregator.GCResultInterface
import io.reactivex.Observable

interface FetchLocations {
  fun getLocations(parameters: FetchLocationsParameters): Observable<List<GCResultInterface>>
}
