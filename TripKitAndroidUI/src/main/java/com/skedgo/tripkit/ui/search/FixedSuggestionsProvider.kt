package com.skedgo.tripkit.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.ui.R


interface FixedSuggestionsProvider {
    fun fixedSuggestions(context: Context, iconProvider: LocationSearchIconProvider): List<SearchSuggestion>
}

enum class DefaultFixedSuggestionType {
    CURRENT_LOCATION,
    CHOOSE_ON_MAP
}

class DefaultFixedSuggestionsProvider(val showCurrentLocation: Boolean, val showChooseOnMap: Boolean) : FixedSuggestionsProvider {
    override fun fixedSuggestions(context: Context, iconProvider: LocationSearchIconProvider): List<SearchSuggestion> {
        val currentLocation = DefaultSearchSuggestion(DefaultFixedSuggestionType.CURRENT_LOCATION, context.getString(R.string.current_location),
                                                     null,
                                                     R.color.title_text,
                                                     R.color.description_text,
                                                     ContextCompat.getDrawable(context,
                                                             iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CURRENT_LOCATION))!! )
        val chooseOnMap = DefaultSearchSuggestion(DefaultFixedSuggestionType.CHOOSE_ON_MAP, context.getString(R.string.choose_on_map),
                null,
                R.color.title_text,
                R.color.description_text,
                ContextCompat.getDrawable(context,
                        iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.DROP_PIN))!! )

        val list = mutableListOf<SearchSuggestion>()
        if (showCurrentLocation) {
            list.add(currentLocation)
        }

        if (showChooseOnMap) {
            list.add(chooseOnMap)
        }
        return list
    }

}