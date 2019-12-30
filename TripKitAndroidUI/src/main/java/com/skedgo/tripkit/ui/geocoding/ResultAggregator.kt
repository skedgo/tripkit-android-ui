package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.search.FetchLocationsParameters

// FIXME: Move this to TripGoDomain module.
interface ResultAggregator {
  fun aggregate(
          autocompleteRequest: FetchLocationsParameters,
          results: List<List<GCResultInterface>>
  ): List<Place>
}
