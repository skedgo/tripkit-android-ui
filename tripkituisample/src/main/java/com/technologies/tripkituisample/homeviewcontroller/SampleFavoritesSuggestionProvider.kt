package com.technologies.tripkituisample.homeviewcontroller

import android.content.Context
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUIFavoritesSuggestionProvider
import com.skedgo.tripkit.ui.search.DefaultSearchSuggestion
import com.skedgo.tripkit.ui.search.LocationSearchIconProvider
import com.skedgo.tripkit.ui.search.SearchSuggestion
import com.technologies.tripkituisample.R

/**
 * By extending TKUIFavoritesSuggestionProvider, you can control
 * favorites data to fetch, save and remove from storage of your choice
 */
class SampleFavoritesSuggestionProvider : TKUIFavoritesSuggestionProvider() {

    private val sampleFavorites = mutableListOf<SampleFavoriteData>()
    private var homeFavorite: SampleFavoriteData? = null
    private var workFavorite: SampleFavoriteData? = null

    override suspend fun onClick(id: Any) {
        super.onClick(id)
    }

    override suspend fun query(
        context: Context,
        iconProvider: LocationSearchIconProvider,
        query: String
    ): List<SearchSuggestion> {

        val resultType = LocationSearchIconProvider.SearchResultType.FAVORITE
        var drawableId = iconProvider.iconForSearchResult(resultType, null)
        if (drawableId == 0) {
            drawableId = R.drawable.ic_favorite
        }

        val result = mutableListOf<DefaultSearchSuggestion>()

        sampleFavorites.forEach {
            it.location.isFavourite(true)
            result.add(
                DefaultSearchSuggestion(
                    it.id,
                    (it.additionalData as SampleAdditionalData).title,
                    (it.additionalData as SampleAdditionalData).address,
                    R.color.black,
                    R.color.black1,
                    ContextCompat.getDrawable(context, drawableId)!!,
                    it.location
                )
            )
        }

        return result
    }

    override fun saveHome(location: Location, additionalData: Any?) {
        //super.saveHome(location, additionalData)

        location.address = (additionalData as SampleAdditionalData).address

        homeFavorite = SampleFavoriteData(
                HOME_ID,
                location,
                additionalData
            )
    }

    override fun getHome(): Location? {
        //return super.getHome()
        return homeFavorite?.run {
            location.id = id.toLong()
            location.address = (additionalData as SampleAdditionalData).address
            location
        }
    }

    override fun saveWork(location: Location, additionalData: Any?) {
        //super.saveWork(location, additionalData)

        location.address = (additionalData as SampleAdditionalData).address

        workFavorite = SampleFavoriteData(
            WORK_ID,
            location,
            additionalData
        )
    }

    override fun getWork(): Location? {
        //return super.getWork()
        return workFavorite?.run {
            location.id = id.toLong()
            location.address = (additionalData as SampleAdditionalData).address
            location
        }
    }

    override fun getFavorite(id: Any): Location? {
        return sampleFavorites.firstOrNull { it.id == id }?.run {
            location.id = this.id.toLong()
            location.address = (additionalData as SampleAdditionalData).address
            location
        }
    }

    override fun saveFavorite(location: Location, id: Any, additionalData: Any?) {
        //super.saveFavorite(location, id, additionalData)

        location.address = (additionalData as SampleAdditionalData).address

        sampleFavorites.add(
            SampleFavoriteData(
                id as Int,
                location,
                additionalData
            )
        )
    }

    override fun removeFavorite(id: Any) {
        //super.removeFavorite(id)
        sampleFavorites.removeAll { it.id == id as Int }
    }

    data class SampleAdditionalData(
        val title: String,
        val address: String = ""
    )

    data class SampleFavoriteData(
        val id: Int,
        val location: Location,
        val additionalData: Any?
    )

    companion object {
        const val HOME_ID = 1001
        const val WORK_ID = 1002
    }
}