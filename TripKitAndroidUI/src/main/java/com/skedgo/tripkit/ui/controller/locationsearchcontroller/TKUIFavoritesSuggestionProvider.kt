package com.skedgo.tripkit.ui.controller.locationsearchcontroller

import android.content.Context
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.search.LocationSearchIconProvider
import com.skedgo.tripkit.ui.search.LocationSearchProvider
import com.skedgo.tripkit.ui.search.SearchSuggestion


open class TKUIFavoritesSuggestionProvider : LocationSearchProvider {

    override suspend fun onClick(id: Any) {}

    override suspend fun query(
        context: Context,
        iconProvider: LocationSearchIconProvider,
        query: String
    ): List<SearchSuggestion> {
        return emptyList()
    }

    open fun saveFavorite(location: Location, id: Any, additionalData: Any? = null) {}

    open fun getFavorite(id: Any): Location? {
        return null
    }

    open fun saveWork(location: Location, additionalData: Any? = null) {}
    open fun getWork(): Location? {
        return null
    }

    open fun getHome(): Location? {
        return null
    }

    open fun saveHome(location: Location, additionalData: Any? = null) {}

    open fun removeFavorite(id: Any) {}

    open fun isFavoriteExist(id: Any): Boolean {
        return false
    }
}