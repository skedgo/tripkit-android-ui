package com.technologies.tripkituisample.homeviewcontroller

import android.content.Context
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUIFavoritesSuggestionProvider
import com.skedgo.tripkit.ui.search.LocationSearchIconProvider
import com.skedgo.tripkit.ui.search.SearchSuggestion

/**
 * By extending TKUIFavoritesSuggestionProvider, you can control
 * favorites data to fetch, save and remove from your local storage
 */
class SampleFavoritesSuggestionProvider: TKUIFavoritesSuggestionProvider() {

    override suspend fun onClick(id: Any) {
        super.onClick(id)
    }

    override suspend fun query(
        context: Context,
        iconProvider: LocationSearchIconProvider,
        query: String
    ): List<SearchSuggestion> {
        return super.query(context, iconProvider, query)
    }

    override fun saveHome(location: Location, additionalData: Any?) {
        super.saveHome(location, additionalData)
    }

    override fun getHome(): Pair<Location, Any?>? {
        return super.getHome()
    }

    override fun saveWork(location: Location, additionalData: Any?) {
        super.saveWork(location, additionalData)
    }

    override fun getWork(): Pair<Location, Any?>? {
        return super.getWork()
    }

    override fun getFavorite(id: Any): Pair<Location, Any?>? {
        return super.getFavorite(id)
    }

    override fun saveFavorite(location: Location, id: Any, additionalData: Any?) {
        super.saveFavorite(location, id, additionalData)
    }

    override fun removeFavorite(id: Any) {
        super.removeFavorite(id)
    }
}