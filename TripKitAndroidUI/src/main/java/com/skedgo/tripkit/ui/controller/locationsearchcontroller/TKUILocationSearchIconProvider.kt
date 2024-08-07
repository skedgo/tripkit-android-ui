package com.skedgo.tripkit.ui.controller.locationsearchcontroller

import com.skedgo.tripkit.common.model.StopType
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.search.LocationSearchIconProvider

class TKUILocationSearchIconProvider : LocationSearchIconProvider {
    override fun iconForSearchResult(
        resultType: LocationSearchIconProvider.SearchResultType,
        stopType: StopType?
    ): Int {
        return when (resultType) {
            LocationSearchIconProvider.SearchResultType.CURRENT_LOCATION -> R.drawable.ic_search_current_location
            LocationSearchIconProvider.SearchResultType.DROP_PIN -> R.drawable.ic_search_choose_on_map
            LocationSearchIconProvider.SearchResultType.SCHEDULED_STOP -> getIconForScheduledStop(
                stopType
            )
            LocationSearchIconProvider.SearchResultType.CONTACT -> R.drawable.ic_contact_search
            LocationSearchIconProvider.SearchResultType.CALENDAR -> R.drawable.ic_search_calendar
            LocationSearchIconProvider.SearchResultType.W3W -> R.drawable.icon_what3word_gray
            LocationSearchIconProvider.SearchResultType.HOME -> R.drawable.ic_search_home
            LocationSearchIconProvider.SearchResultType.WORK -> R.drawable.ic_search_work
            LocationSearchIconProvider.SearchResultType.FOURSQUARE -> R.drawable.ic_foursquare_search
            LocationSearchIconProvider.SearchResultType.GOOGLE -> R.drawable.ic_search_pin
            else -> 0
        }
    }

    private fun getIconForScheduledStop(stopType: StopType?): Int {

        return when (stopType) {
            StopType.BUS -> R.drawable.ic_search_bus
            StopType.TRAIN -> R.drawable.ic_search_train
            StopType.FERRY -> R.drawable.ic_search_ferry
            StopType.MONORAIL -> R.drawable.ic_search_monorail
            StopType.SUBWAY -> R.drawable.ic_search_subway
            StopType.TAXI -> R.drawable.ic_search_taxi
            StopType.PARKING -> R.drawable.ic_search_parking
            StopType.TRAM -> R.drawable.ic_search_tram
            StopType.CABLECAR -> R.drawable.ic_search_cablecar
            else -> 0
        }
    }
}