package com.skedgo.tripkit.ui.map
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ViewableInfoWindowAdapter
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single
import com.skedgo.tripkit.locations.CarPod

class CarPodPOILocation(private val carPod: CarPod) : POILocation {

  override fun createMarkerOptions(resources: Resources, picasso: Picasso): Single<MarkerOptions> {
    return CreateMarkerForCarPod.execute(resources, picasso, carPod)
  }

  override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter? {
    return ViewableInfoWindowAdapter(LayoutInflater.from(context))
  }

  override fun toLocation(): Location {
    val location = Location(carPod.lat, carPod.lng)
    location.name = carPod.name
    location.address = carPod.address
    location.phoneNumber = carPod.operator.phone
    return location
  }

  override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {
//    val event = Event.TransportIconSelected(null)
//    eventTracker.log(event)
  }


  override val identifier: String = carPod.id
}