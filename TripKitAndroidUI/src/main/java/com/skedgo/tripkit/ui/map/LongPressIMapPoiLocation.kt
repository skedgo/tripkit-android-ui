package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single


class LongPressIMapPoiLocation(
    val point: LatLng,
    private val stopInfoWindowAdapter: StopInfoWindowAdapter
) : IMapPoiLocation {
    private val location: Location = Location()

    init {
        location.lat = point.latitude
        location.lon = point.longitude
        location.name = "${point.latitude}, ${point.longitude}"
    }

    fun setName(name: String) {
        location.name = name
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

    override val identifier: String = "${point.latitude},${point.longitude}"
}