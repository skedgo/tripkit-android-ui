package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.location.PoiLocation
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single


class GenericIMapPoiLocation(
    private val poiLocation: PointOfInterest,
    private val placeId: String,
    private val stopInfoWindowAdapter: StopInfoWindowAdapter
) : IMapPoiLocation {
    private val location: Location

    init {
        location = PoiLocation()
        location.placeId = placeId
        location.lat = poiLocation.latLng.latitude
        location.lon = poiLocation.latLng.longitude
        location.name = poiLocation.name
    }

    override fun createMarkerOptions(
        resources: Resources,
        picasso: Picasso
    ): Single<MarkerOptions> {
        return Single.create { MarkerOptions() }
    }

    override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter? {
        return stopInfoWindowAdapter
    }

    override fun toLocation(): Location = location

    override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {
    }

    override val identifier: String = poiLocation.placeId
}