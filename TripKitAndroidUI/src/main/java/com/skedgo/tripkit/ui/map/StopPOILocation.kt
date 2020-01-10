package com.skedgo.tripkit.ui.map
import android.content.Context
import android.content.res.Resources
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single

class StopPOILocation(
    val scheduledStop: ScheduledStop,
    private val stopInfoWindowAdapter: StopInfoWindowAdapter) : IMapPoiLocation {

  override fun createMarkerOptions(resources: Resources, picasso: Picasso): Single<MarkerOptions> {
    return scheduledStop.createStopMarkerOptions()
  }

  override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter? {
    return stopInfoWindowAdapter
  }

  override fun toLocation(): Location {
    return scheduledStop
  }

  override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {
  }

  override val identifier: String = scheduledStop.code
}