package com.skedgo.tripkit.ui.geocoding

import com.skedgo.tripkit.ui.TripKitUI

open class RegionalGeocoder : Geocoder() {
    override val serviceUrl: String
        get() {
            val latitude = nearLatitude
            val longitude = nearLongitude
            // This can trigger a regions refresh (network). Never let a DNS outage crash the app.
            val region = TripKitUI.getInstance()
                .regionService()
                .getRegionByLocationAsync(latitude, longitude)
                .onErrorResumeNext(io.reactivex.Observable.empty())
                .firstElement()
                .blockingGet()
            val urls: List<String> = region.getURLs().orEmpty()
            return urls.firstOrNull().orEmpty()
        }
}
