package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.content.Context
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.search.DefaultFixedSuggestionType
import com.skedgo.tripkit.ui.search.DefaultSearchSuggestion
import com.skedgo.tripkit.ui.search.FixedSuggestions
import com.skedgo.tripkit.ui.search.FixedSuggestionsProvider
import com.skedgo.tripkit.ui.search.LegacyLocationSearchIconProvider
import com.skedgo.tripkit.ui.search.LocationSearchIconProvider
import com.skedgo.tripkit.ui.search.SearchSuggestion

class TKUIHomeViewFixedSuggestionsProvider() : FixedSuggestionsProvider {

    var showCurrentLocation = true

    override fun fixedSuggestions(context: Context, iconProvider: LocationSearchIconProvider): List<SearchSuggestion> {
        val currentLocation = DefaultSearchSuggestion(
            FixedSuggestions.CURRENT_LOCATION, context.getString(R.string.current_location),
                null,
                R.color.tripKitSuccess,
                R.color.description_text,
                ContextCompat.getDrawable(context,
                        iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CURRENT_LOCATION))!! )
        val chooseOnMap = DefaultSearchSuggestion(
            FixedSuggestions.CHOOSE_ON_MAP, context.getString(R.string.choose_on_map),
                null,
                R.color.title_text,
                R.color.description_text,
                ContextCompat.getDrawable(context,
                        iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.DROP_PIN))!! )
        val home = DefaultSearchSuggestion(
            FixedSuggestions.HOME, context.getString(R.string.home),
                null,
                R.color.title_text,
                R.color.description_text,
                ContextCompat.getDrawable(context,
                        iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.HOME))!! )
        val work = DefaultSearchSuggestion(
            FixedSuggestions.WORK, context.getString(R.string.work),
                null,
                R.color.title_text,
                R.color.description_text,
                ContextCompat.getDrawable(context,
                        iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.WORK))!! )
        if (showCurrentLocation) {
            return listOf(currentLocation, chooseOnMap, home, work)
        } else {
            return listOf(chooseOnMap, home, work)
        }
    }

    override fun locationsToSuggestion(context: Context, locations: List<Location>, iconProvider: LegacyLocationSearchIconProvider): List<SearchSuggestion> {
        val list = mutableListOf<SearchSuggestion>()
        locations.forEach {
            list.add(DefaultSearchSuggestion(
                    it.address,
                    it.name ?: it.address,
                    it.address,
                    R.color.title_text,
                    R.color.description_text,
                    ContextCompat.getDrawable(context, iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.HISTORY))!!,
                    it
            ))
        }

        return list
    }

    override fun citiesToSuggestion(context: Context, locations: List<Region.City>, iconProvider: LegacyLocationSearchIconProvider): List<SearchSuggestion> {
        val list = mutableListOf<SearchSuggestion>()
        locations.forEach {
            list.add(DefaultSearchSuggestion(
                    it.address ?: it.name,
                    it.name,
                    it.address ?: null,
                    R.color.title_text,
                    R.color.description_text,
                    ContextCompat.getDrawable(context, iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.REGION))!!,
                    it
            ))
        }
        return list
    }

    override fun specificSuggestions(context: Context, suggestionTypes: List<DefaultFixedSuggestionType>, iconProvider: LocationSearchIconProvider): List<SearchSuggestion> {
        val list = mutableListOf<SearchSuggestion>()

        if (suggestionTypes.contains(DefaultFixedSuggestionType.CURRENT_LOCATION)) {
            list.add(
                    DefaultSearchSuggestion(
                        FixedSuggestions.CURRENT_LOCATION, context.getString(R.string.current_location),
                            null,
                            R.color.tripKitSuccess,
                            R.color.description_text,
                            ContextCompat.getDrawable(context,
                                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.CURRENT_LOCATION))!! )
            )
        }

        if (suggestionTypes.contains(DefaultFixedSuggestionType.CHOOSE_ON_MAP)) {
            list.add(
                    DefaultSearchSuggestion(
                        FixedSuggestions.CHOOSE_ON_MAP, context.getString(R.string.choose_on_map),
                            null,
                            R.color.title_text,
                            R.color.description_text,
                            ContextCompat.getDrawable(context,
                                    iconProvider.iconForSearchResult(LocationSearchIconProvider.SearchResultType.DROP_PIN))!! )
            )
        }

        return list
    }
}