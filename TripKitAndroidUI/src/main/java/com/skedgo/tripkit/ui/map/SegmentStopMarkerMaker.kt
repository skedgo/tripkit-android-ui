package com.skedgo.tripkit.ui.map
import android.content.Context
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.ui.R
import javax.inject.Inject

class SegmentStopMarkerMaker @Inject internal constructor(private val context: Context) {
  fun make(stopMarkerViewModel: StopMarkerViewModel): MarkerOptions {
    val icon = MapMarkerUtils.createStopMarkerIcon(
        context.resources.getDimensionPixelSize(R.dimen.stop_circle_pin_diameter),
        stopMarkerViewModel.strokeColor,
        stopMarkerViewModel.fillColor,
        !stopMarkerViewModel.isTravelled
    )
    return MarkerOptions()
        .title(stopMarkerViewModel.title)
        .snippet(stopMarkerViewModel.snippet)
        .position(stopMarkerViewModel.position)
        .draggable(false)
        .alpha(stopMarkerViewModel.alpha)
        .icon(BitmapDescriptorFactory.fromBitmap(icon))
        .anchor(0.5f, 0.5f)
        .infoWindowAnchor(0.5f, 0f)
  }
}
