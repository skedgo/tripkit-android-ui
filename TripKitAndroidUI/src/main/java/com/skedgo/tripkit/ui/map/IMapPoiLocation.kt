package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single

interface IMapPoiLocation {
    fun createMarkerOptions(resources: Resources, picasso: Picasso): Single<MarkerOptions>
    fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter?
    fun toLocation(): Location
    fun onMarkerClick(bus: Bus, eventTracker: EventTracker)

    val identifier: String

//  companion object {
//    fun handleInfoWindowClick(context: Context, location: Location, locationId: String): Intent {
//      return Intent(context, ViewLocationActivity::class.java)
//          .putExtra(ViewLocationActivity.EXTRA_LOCATION, location)
//          .putExtra(ViewLocationActivity.LOCATION_ID, locationId)
//          .putExtra(ViewLocationActivity.EXTRA_IS_FROM_MARKER, false)
//    }
//  }
}