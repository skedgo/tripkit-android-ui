package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCResultInterface
import io.reactivex.Observable

interface FilterSupportedLocations {
  operator fun invoke(
      results: List<GCResultInterface>
  ): Observable<List<GCResultInterface>>
}
