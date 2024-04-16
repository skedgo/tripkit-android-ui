package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.data.database.locations.facility.FacilityLocationEntity
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ViewableInfoWindowAdapter
import com.skedgo.tripkit.ui.model.PodLocation
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single

class FacilityPOILocation(
    val facilityLocationEntity: FacilityLocationEntity) : IMapPoiLocation {
  override fun createMarkerOptions(resources: Resources, picasso: Picasso): Single<MarkerOptions> =
    CreateMarkerFoFacility.execute(resources, facilityLocationEntity)

  override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter {
    return ViewableInfoWindowAdapter(LayoutInflater.from(context))

  }

  override fun toLocation(): Location {
    val location = PodLocation(facilityLocationEntity.lat, facilityLocationEntity.lng)
    location.podIdentifier = facilityLocationEntity.identifier
    location.name = facilityLocationEntity.name
    location.address = facilityLocationEntity.address
    location.timeZone = facilityLocationEntity.timezone
    return location
  }

  override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {

  }

  override val identifier: String = facilityLocationEntity.identifier

}