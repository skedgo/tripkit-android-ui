package com.skedgo.tripkit.ui.controller.locationsearchcontroller

import android.content.Context
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.search.LocationSearchIconProvider
import com.skedgo.tripkit.ui.search.LocationSearchProvider
import com.skedgo.tripkit.ui.search.SearchSuggestion
import timber.log.Timber
import javax.inject.Inject


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

    open fun getFavorite(id: Any): Pair<Location, Any?>? {
        return null
    }

    open fun saveWork(location: Location, additionalData: Any? = null) {}
    open fun getWork(): Pair<Location, Any?>? {
        return null
    }

    open fun getHome(): Pair<Location, Any?>? {
        return null
    }

    open fun saveHome(location: Location, additionalData: Any? = null) {}

    open fun removeFavorite(id: Any) {}

    open fun isFavoriteExist(id: Any): Boolean {
        return false
    }
}