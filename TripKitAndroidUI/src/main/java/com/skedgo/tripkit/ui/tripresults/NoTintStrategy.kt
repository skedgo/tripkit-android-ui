package com.skedgo.tripkit.ui.tripresults

import android.graphics.drawable.Drawable
import com.skedgo.tripkit.routing.ServiceColor

object NoTintStrategy : TransportTintStrategy {
  override fun apply(remoteIconIsTemplate: Boolean, serviceColor: ServiceColor?, drawable: Drawable): Drawable {
    // Do nothing
    return drawable
  }
}