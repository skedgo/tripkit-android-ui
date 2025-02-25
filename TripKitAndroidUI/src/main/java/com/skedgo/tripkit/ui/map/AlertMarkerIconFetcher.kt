package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.configuration.ServerManager.configuration
import com.squareup.picasso.Picasso
import dagger.Lazy
import java.lang.ref.WeakReference
import javax.inject.Inject

class AlertMarkerIconFetcher @Inject internal constructor(
    private val resources: Resources,
    private val picassoLazy: Lazy<Picasso>
) {
    fun call(marker: Marker, realtimeAlert: RealtimeAlert) {
        val iconName = realtimeAlert.remoteIcon()
        if (iconName != null) {
            picassoLazy.get().load(IconUtils.asUrl(resources, iconName, URL_TEMPLATE))
                .into(MarkerTarget(WeakReference(marker)))
        }
    }

    companion object {
        private val URL_TEMPLATE =
            configuration.staticTripGoUrl + "icons/android/%s/ic_alert_%s.png"
    }
}
