package com.skedgo.tripkit.ui.search

import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import com.skedgo.tripkit.common.model.StopType
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.BindingConversions

/**
 * When search results are displayed by the LocationSearchFragment, an icon is shown next to each kind of result.
 * If you would like to provide icons in your own style, you can provide the LocationSearchFragment with an icon
 * provider.
 */
interface LocationSearchIconProvider {
    enum class SearchResultType {
        DROP_PIN,
        CURRENT_LOCATION,
        SCHEDULED_STOP,
        CONTACT,
        CALENDAR,
        W3W,
        HOME,
        WORK,
        FOURSQUARE,
        GOOGLE,
        FAVORITE
    }

    /**
     * Implementations of this function should return the resource ID of a drawable to be used for a specific [StopType].
     *
     * @param resultType The type of search result
     * @param stopType The type of stop if resultType is SCHEDULED_STOP
     * @return A drawable resource ID, or 0 if no icon should be displayed
     */
    @DrawableRes
    fun iconForSearchResult(resultType: SearchResultType, stopType: StopType? = null): Int
}

class LegacyLocationSearchIconProvider : LocationSearchIconProvider {
    override fun iconForSearchResult(resultType: LocationSearchIconProvider.SearchResultType, stopType: StopType?): Int {
        return when (resultType) {
            LocationSearchIconProvider.SearchResultType.CURRENT_LOCATION -> R.drawable.ic_currentlocation
            LocationSearchIconProvider.SearchResultType.DROP_PIN -> R.drawable.ic_pin
            LocationSearchIconProvider.SearchResultType.SCHEDULED_STOP -> getIconForScheduledStop(stopType)
            LocationSearchIconProvider.SearchResultType.CONTACT -> R.drawable.ic_contact_search
            LocationSearchIconProvider.SearchResultType.CALENDAR -> R.drawable.ic_calendar_search
            LocationSearchIconProvider.SearchResultType.W3W -> R.drawable.icon_what3word_gray
            LocationSearchIconProvider.SearchResultType.HOME -> R.drawable.home
            LocationSearchIconProvider.SearchResultType.WORK -> R.drawable.work
            LocationSearchIconProvider.SearchResultType.FOURSQUARE -> R.drawable.ic_foursquare_search
            LocationSearchIconProvider.SearchResultType.GOOGLE -> R.drawable.ic_googleresult
            LocationSearchIconProvider.SearchResultType.FAVORITE -> R.drawable.ic_favorite
            else -> 0
        }
    }

    private fun getIconForScheduledStop(stopType: StopType?): Int {
        return BindingConversions.convertStopTypeToMapIconRes(stopType)
    }
}