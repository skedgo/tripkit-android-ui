package com.skedgo.tripkit.ui.map.adapter

import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.SimpleCalloutView
import com.skedgo.tripkit.routing.TripSegment
import java.util.*

class SegmentInfoWindowAdapter(private val inflater: LayoutInflater) : SimpleInfoWindowAdapter() {
    private var segmentCache: HashMap<Marker, TripSegment>? = null

    fun setSegmentCache(segmentCache: HashMap<Marker, TripSegment>) {
        this.segmentCache = segmentCache
    }

    override fun getInfoContents(marker: Marker): View {
        val view = SimpleCalloutView.create(inflater)
        view.setTitle(marker.title)
        view.setSnippet(marker.snippet)

        val segment = segmentCache!![marker]
        if (segment != null) {
            val alerts = segment.alerts
            if (!alerts.isNullOrEmpty()) {
                val iconRes = if (RealtimeAlert.SEVERITY_ALERT == alerts!![0].severity())
                    R.drawable.ic_alert_red_overlay
                else
                    R.drawable.ic_alert_yellow_overlay
                view.setLeftImage(iconRes)
            }
        }

        return view
    }
}