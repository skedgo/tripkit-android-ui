package com.skedgo.tripkit.ui.geocoding
import com.skedgo.tripkit.ui.data.places.Place

sealed class AutoCompleteResult

object NoConnection : AutoCompleteResult()
data class HasResults(val suggestions: List<Place>) : AutoCompleteResult()
data class NoResult(val query: String) : AutoCompleteResult() {
  init {
    require(query.isNotEmpty())
  }
}