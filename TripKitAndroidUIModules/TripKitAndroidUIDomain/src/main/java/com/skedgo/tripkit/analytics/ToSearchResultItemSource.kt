package com.skedgo.tripkit.analytics

import com.skedgo.tripkit.common.model.location.Location

fun String?.toSearchResultItemSource(): SearchResultItemSource = when (this) {
    Location.GOOGLE -> SearchResultItemSource.GoogleLocation
    Location.LOCAL -> SearchResultItemSource.Favorite
    Location.FOURSQUARE -> SearchResultItemSource.Foursquare
    Location.TRIPGO -> SearchResultItemSource.TripGo
    else -> SearchResultItemSource.Unknown
}
