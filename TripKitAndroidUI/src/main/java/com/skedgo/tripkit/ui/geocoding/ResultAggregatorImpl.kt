package com.skedgo.tripkit.ui.geocoding

import androidx.annotation.WorkerThread
import com.skedgo.geocoding.GCBoundingBox
import com.skedgo.geocoding.GCQuery
import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.geocoding.agregator.MGAResultInterface
import com.skedgo.geocoding.agregator.MultiSourceGeocodingAggregator
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import javax.inject.Inject

class ResultAggregatorImpl @Inject internal constructor() : ResultAggregator {
    @WorkerThread
    override fun aggregate(
        autocompleteRequest: FetchLocationsParameters,
        results: List<List<GCResultInterface>>
    ): List<Place> {
        val bounds = GCBoundingBox(
            autocompleteRequest.southwestLat(),
            autocompleteRequest.southwestLon(),
            autocompleteRequest.northeastLat(),
            autocompleteRequest.northeastLon()
        )

        val query = GCQuery(autocompleteRequest.term(), bounds)
        val aggregator: MultiSourceGeocodingAggregator<GCResultInterface> =
            MultiSourceGeocodingAggregator.getInstance()
        val list = aggregator.aggregate(query, results)
        return adaptCGResults(list)
    }

    private fun adaptCGResults(
        results: List<MGAResultInterface<GCResultInterface>>
    ): List<Place> {
        val places = ArrayList<Place>(results.size)
        results.mapTo(places) { (it.result as ResultLocationAdapter<*>).place }
        return places
    }
}
