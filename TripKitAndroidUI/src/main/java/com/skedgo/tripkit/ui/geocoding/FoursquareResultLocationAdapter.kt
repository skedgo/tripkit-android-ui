package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCFoursquareResultInterface
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.data.places.Place.TripGoPOI

class FoursquareResultLocationAdapter(
    private val location: Location,
    private val resultInterface: GCFoursquareResultInterface
) : GCFoursquareResultInterface, ResultLocationAdapter<TripGoPOI?> {
    override fun getPlace(): TripGoPOI {
        return TripGoPOI(location)
    }

    override val isVerified: Boolean
        get() = resultInterface.isVerified

    override val categories: List<String>
        get() = resultInterface.categories

    override val name: String
        get() = resultInterface.name

    override val lat: Double?
        get() = resultInterface.lat

    override val lng: Double?
        get() = resultInterface.lng
}