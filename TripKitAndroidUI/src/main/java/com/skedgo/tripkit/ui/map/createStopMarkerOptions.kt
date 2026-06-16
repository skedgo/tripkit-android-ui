package com.skedgo.tripkit.ui.map

import android.text.TextUtils
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.map.home.MapData
import com.skedgo.tripkit.ui.utils.BindingConversions
import com.squareup.picasso.Picasso
import io.reactivex.Single

fun ScheduledStop.createStopMarkerOptions(): Single<MarkerOptions> {
    val stop = this
    val title = stop.getStopDisplayName()
    val markerOptions = MarkerOptions()
        .title(title)
        .snippet(stop.services)
        .position(LatLng(stop.lat, stop.lon))
        .draggable(false)

    return Single.fromCallable {
        val iconRes = BindingConversions.convertStopTypeToMapIconRes(stop.type)
        val icon: BitmapDescriptor = if (iconRes == 0) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        } else {
            BitmapDescriptorFactory.fromResource(iconRes)
        }
        markerOptions.icon(icon)
        markerOptions
    }
}
fun ScheduledStop.createStopMarkerOptions(picasso: Picasso): Single<MarkerOptions> {
    val stop = this
    val title = stop.getStopDisplayName()
    val markerOptions = MarkerOptions()
        .title(title)
        .snippet(stop.services)
        .position(LatLng(stop.lat, stop.lon))
        .draggable(false)
    return kotlin.run {
        val remoteMarkerIconFetcher = RemoteMarkerIconFetcher(picasso)
        remoteMarkerIconFetcher.callAsync(markerOptions, stop)
    }.map {
        if (MapData.isRegionalSourceStop(code)) {
            MapData.addRegionalStop(it, this)
        }
        it
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
