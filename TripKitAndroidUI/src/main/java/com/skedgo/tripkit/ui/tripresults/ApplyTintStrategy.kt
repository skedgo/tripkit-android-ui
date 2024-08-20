package com.skedgo.tripkit.ui.tripresults

import android.graphics.drawable.Drawable
import com.skedgo.tripkit.ui.utils.tint
import com.skedgo.tripkit.routing.ServiceColor
import com.skedgo.tripkit.routing.toInt

object ApplyTintStrategy : TransportTintStrategy {
    override fun apply(
        remoteIconIsTemplate: Boolean,
        remoteIconIsBranding: Boolean,
        serviceColor: ServiceColor?,
        drawable: Drawable
    ): Drawable {
        if (serviceColor != null && remoteIconIsTemplate && !remoteIconIsBranding) {
            return drawable.tint(serviceColor.toInt(0xff))
        }
        return drawable
    }
}