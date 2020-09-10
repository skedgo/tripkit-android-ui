package com.skedgo.tripkit.ui.map
import android.content.Context
import android.content.res.Resources
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.ui.map.adapter.FreeFloatingVehicleInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.model.PodLocation
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single

class FreeFloatingVehiclePOILocation(
    val freeFloatingLocationEntity: FreeFloatingLocationEntity) : IMapPoiLocation {
  override fun createMarkerOptions(resources: Resources, picasso: Picasso): Single<MarkerOptions> =
      CreateMarkerForFreeFloatingLocation.execute(resources, freeFloatingLocationEntity)

  override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter? {
    return FreeFloatingVehicleInfoWindowAdapter(context)

  }

  override fun toLocation(): Location {
    val location = PodLocation(freeFloatingLocationEntity.lat, freeFloatingLocationEntity.lng)
    location.podIdentifier = freeFloatingLocationEntity.identifier
    location.phoneNumber = freeFloatingLocationEntity.vehicle.operator.phone
    location.name = freeFloatingLocationEntity.vehicle.operator.name
    location.address = freeFloatingLocationEntity.address
    location.url = freeFloatingLocationEntity.vehicle.operator.website
    return location
  }

  override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {
//    val event = Event.TransportIconSelected(null)
//    eventTracker.log(event)
  }

  override val identifier: String = freeFloatingLocationEntity.identifier

}