package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.location.PoiLocation
import com.skedgo.tripkit.ui.map.adapter.NonClickableInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Single

/**
 * Lightweight cached POI used for regional-level stop markers restored from cache.
 * Carries enough info for info window rendering and click handling without full ScheduledStop.
 */
class CachedStopPoiLocation(
    private val title: String?,
    private val snippet: String?,
    private val lat: Double,
    private val lon: Double
) : IMapPoiLocation {
    private val location: PoiLocation = PoiLocation().apply {
        this.name = title
        this.lat = this@CachedStopPoiLocation.lat
        this.lon = this@CachedStopPoiLocation.lon
    }

    override fun createMarkerOptions(resources: Resources, picasso: Picasso): Single<MarkerOptions> {
        // Not used in restoration path; marker options are already provided by cache.
        return Single.create { emitter -> emitter.onSuccess(MarkerOptions()) }
    }

    override fun getInfoWindowAdapter(context: Context): StopInfoWindowAdapter? {
        // Non-clickable info window consistent with StopPOILocation default
        return NonClickableInfoWindowAdapter(android.view.LayoutInflater.from(context))
    }

    override fun toLocation(): Location = location

    override fun onMarkerClick(bus: Bus, eventTracker: EventTracker) {
        // No-op for cached entries
    }

    override val identifier: String = "${title ?: ""}@$lat,$lon"
}


