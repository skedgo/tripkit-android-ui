package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.utils.StopMarkerUtils.getMapIconUrlForModeInfo
import com.squareup.picasso.Picasso
import java.lang.ref.WeakReference
import javax.inject.Inject

class StopMarkerIconFetcher @Inject constructor(
    private val resources: Resources,
    private val picasso: Picasso
) {
    fun call(marker: Marker, modeInfo: ModeInfo?) {
        if (modeInfo != null) {
            val url = getMapIconUrlForModeInfo(resources, modeInfo)
            picasso.load(url).into(MarkerTarget(WeakReference(marker)))
        }
    }
}
