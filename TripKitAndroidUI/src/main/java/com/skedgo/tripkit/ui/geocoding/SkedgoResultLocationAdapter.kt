package com.skedgo.tripkit.ui.geocoding

import com.skedgo.geocoding.agregator.GCSkedGoResultInterface
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.data.places.Place.TripGoPOI

class SkedgoResultLocationAdapter(
    private val location: Location,
    private val resultInterface: GCSkedGoResultInterface
) : GCSkedGoResultInterface, ResultLocationAdapter<TripGoPOI?> {
    override fun getPlace(): TripGoPOI {
        return TripGoPOI(location)
    }

    override val name: String
        get() = resultInterface.name

    override val lat: Double?
        get() = resultInterface.lat

    override val lng: Double?
        get() = resultInterface.lng

    override val resultClass: String
        get() = resultInterface.resultClass

    override val popularity: Int
        get() = resultInterface.popularity

    override val modeIdentifiers: List<String>?
        get() = resultInterface.modeIdentifiers
}