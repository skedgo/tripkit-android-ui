package com.skedgo.tripkit.ui.search

import com.skedgo.geocoding.agregator.GCResultInterface
import io.reactivex.Observable

interface FetchLocalLocations : FetchLocations {
  override fun getLocations(
          parameters: FetchLocationsParameters
  ): Observable<List<GCResultInterface>>
}
