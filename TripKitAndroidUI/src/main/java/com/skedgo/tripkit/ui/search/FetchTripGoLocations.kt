package com.skedgo.tripkit.ui.search

import com.skedgo.geocoding.agregator.GCResultInterface
import io.reactivex.Observable

interface FetchTripGoLocations : FetchLocations {
    override fun getLocations(
        parameters: FetchLocationsParameters
    ): Observable<List<GCResultInterface>>
}
