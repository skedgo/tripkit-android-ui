package com.skedgo.tripkit.ui.controller.locationsearchcontroller

import android.content.Context
import com.skedgo.tripkit.ui.search.LocationSearchIconProvider
import com.skedgo.tripkit.ui.search.LocationSearchProvider
import com.skedgo.tripkit.ui.search.SearchSuggestion
import timber.log.Timber
import javax.inject.Inject


class TKUIFavoritesSuggestionProvider: LocationSearchProvider {
    override suspend fun onClick(id: Any) {

    }

    override suspend fun query(context: Context, iconProvider: LocationSearchIconProvider, query: String): List<SearchSuggestion> {
        return emptyList()
    }
}