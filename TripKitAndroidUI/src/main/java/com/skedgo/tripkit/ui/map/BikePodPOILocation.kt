package com.skedgo.tripkit.ui.map
import android.content.Context
import android.content.res.Resources
import com.gojuno.koptional.toOptional
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodLocationEntity
import com.skedgo.tripkit.ui.map.adapter.BikePodInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.model.PodLocation
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single

class BikePodPOILocation(
    val bikePodEntity: BikePodLocationEntity) : IMapPoiLocation {
  override fun createMarkerOptions(resources: Resources, picasso: Picasso): Single<MarkerOptions> =
      CreateMarkerForBikePod.execute(resources, bikePodEntity)

  override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter? {
    return BikePodInfoWindowAdapter(context)

  }

  override fun toLocation(): Location {
    val location = PodLocation(bikePodEntity.lat, bikePodEntity.lng)
    location.podIdentifier = bikePodEntity.identifier
    location.phoneNumber = bikePodEntity.bikePod.operator.phone
    location.name = bikePodEntity.bikePod.operator.name
    location.address = bikePodEntity.address
    location.url = bikePodEntity.bikePod.operator.website
    location.appUrl = bikePodEntity.bikePod.operator.appInfo?.appURLAndroid
    location.isWithExternalApp = !bikePodEntity.bikePod.operator.appInfo?.appURLAndroid.isNullOrEmpty()
    return location
  }

  override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {
//    val event = Event.TransportIconSelected(null)
//    eventTracker.log(event)
  }

  override val identifier: String = bikePodEntity.identifier

}