package com.skedgo.tripkit.ui.map
import android.text.TextUtils
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.utils.BindingConversions
import io.reactivex.Single

fun ScheduledStop.createStopMarkerOptions(): Single<MarkerOptions> {
  return Single.fromCallable {
    val stop = this
    val title = stop.getStopDisplayName()

    val markerOptions = MarkerOptions()
    markerOptions.title(title)
    markerOptions.snippet(stop.services)
    markerOptions.position(LatLng(stop.lat, stop.lon))
    markerOptions.draggable(false)

    val iconRes = BindingConversions.convertStopTypeToMapIconRes(stop.type)
    val icon: BitmapDescriptor
    if (iconRes == 0) {
      icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
    } else {
      icon = BitmapDescriptorFactory.fromResource(iconRes)
    }
    markerOptions.icon(icon)
  }
}

fun ScheduledStop.getStopDisplayName(): String? {
  var title = this.name
  if (TextUtils.isEmpty(title)) {
    title = this.address
    if (TextUtils.isEmpty(title)) {
      title = this.shortName
      if (TextUtils.isEmpty(title)) {
        title = this.type.toString()
      }
    }
  }
  return title
}
