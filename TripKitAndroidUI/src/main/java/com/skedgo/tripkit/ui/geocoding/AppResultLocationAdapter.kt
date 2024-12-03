package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCAppResultInterface
import com.skedgo.geocoding.agregator.GCAppResultInterface.Source
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.data.places.Place.TripGoPOI

class AppResultLocationAdapter(
    private val location: Location,
    private val resultInterface: GCAppResultInterface
) : ResultLocationAdapter<TripGoPOI?>, GCAppResultInterface {
    override fun getPlace(): TripGoPOI {
        return TripGoPOI(location)
    }

    override val subtitle: String
        get() = resultInterface.subtitle

    override val appResultSource: Source
        get() = resultInterface.appResultSource

    override val isFavourite: Boolean
        get() = resultInterface.isFavourite

    override val name: String
        get() = resultInterface.name

    override val lat: Double?
        get() = resultInterface.lat

    override val lng: Double?
        get() = resultInterface.lng
}