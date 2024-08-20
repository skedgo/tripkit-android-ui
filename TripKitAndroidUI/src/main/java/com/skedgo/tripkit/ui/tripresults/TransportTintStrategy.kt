package com.skedgo.tripkit.ui.tripresults

import android.graphics.drawable.Drawable
import com.skedgo.tripkit.routing.ServiceColor

interface TransportTintStrategy {
    fun apply(
        remoteIconIsTemplate: Boolean,
        remoteIconIsBranding: Boolean,
        serviceColor: ServiceColor?,
        drawable: Drawable
    ): Drawable
}