package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCGoogleResultInterface
import com.skedgo.tripkit.ui.data.places.Place.WithoutLocation

class GoogleResultLocationAdapter(
    private val location: WithoutLocation,
    private val resultInterface: GCGoogleResultInterface
) : GCGoogleResultInterface, ResultLocationAdapter<WithoutLocation?> {
    override fun getPlace(): WithoutLocation {
        return location
    }

    override val name: String
        get() = resultInterface.name

    override val lat: Double?
        get() = resultInterface.lat

    override val lng: Double?
        get() = resultInterface.lng

    override val address: String?
        get() = resultInterface.address
}