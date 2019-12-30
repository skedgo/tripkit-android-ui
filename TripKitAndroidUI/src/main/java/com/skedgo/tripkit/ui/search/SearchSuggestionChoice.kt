package com.skedgo.tripkit.ui.search

import com.skedgo.tripkit.ui.data.places.Place

sealed class SearchSuggestionChoice {
  data class PlaceChoice(val place: Place, val position: Int) : SearchSuggestionChoice()
  data class FixedChoice(val position: Int) : SearchSuggestionChoice() {
    init {
      require(position == 0 || position == 1)
    }
  }
}
