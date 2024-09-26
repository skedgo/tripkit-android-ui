package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.parkingspots.models.Parking
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ViewableInfoWindowAdapter
import com.skedgo.tripkit.ui.model.PodLocation
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single

class CarParkPOILocation(val parking: Parking) : IMapPoiLocation {
    override fun createMarkerOptions(
        resources: Resources,
        picasso: Picasso
    ): Single<MarkerOptions> =
        CreateMarkerForParking.execute(resources, parking)

    override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter {
        return ViewableInfoWindowAdapter(LayoutInflater.from(context))

    }

    override fun toLocation(): Location {

        val location = PodLocation(parking.location.latitude, parking.location.longitude)
        location.podIdentifier = parking.id
        location.name = parking.name
        location.address = parking.address
        return location
    }

    override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {}

    override val identifier: String = parking.id

}
