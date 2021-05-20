package com.skedgo.tripkit.ui.search

import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.data.places.Place

sealed class SearchSuggestionChoice {
  data class PlaceChoice(val place: Place) : SearchSuggestionChoice()
  data class FixedChoice(val id: Any, val location: Location? = null) : SearchSuggestionChoice()
  data class SearchProviderChoice(val location: Location?) : SearchSuggestionChoice()
  data class CityProviderChoice(val location: Location?) : SearchSuggestionChoice()
}
