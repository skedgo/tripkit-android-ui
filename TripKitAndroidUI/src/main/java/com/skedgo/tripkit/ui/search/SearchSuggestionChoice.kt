package com.skedgo.tripkit.ui.search

import com.skedgo.tripkit.ui.data.places.Place

sealed class SearchSuggestionChoice {
  data class PlaceChoice(val place: Place) : SearchSuggestionChoice()
  data class FixedChoice(val id: Any) : SearchSuggestionChoice()
}
