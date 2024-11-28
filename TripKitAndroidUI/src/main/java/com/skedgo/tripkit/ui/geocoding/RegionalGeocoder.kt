package com.skedgo.tripkit.ui.geocoding

import com.skedgo.tripkit.ui.TripKitUI

open class RegionalGeocoder : Geocoder() {
    override val serviceUrl: String
        get() {
            val latitude = nearLatitude
            val longitude = nearLongitude
            val region = TripKitUI.getInstance().regionService()
                .getRegionByLocationAsync(latitude, longitude)
                .blockingFirst()
            val urls: List<String> = region.getURLs().orEmpty()
            return urls.firstOrNull().orEmpty()
        }
}
