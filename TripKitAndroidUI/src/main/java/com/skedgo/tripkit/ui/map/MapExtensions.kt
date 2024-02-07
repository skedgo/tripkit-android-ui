package com.skedgo.tripkit.ui.map

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.ui.R

fun Context.getGeofenceZone(
    lat: Double,
    lon: Double,
    radius: Double
): CircleOptions = CircleOptions()
    .center(LatLng(lat, lon))
    .radius(radius)
    .strokeColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
    .fillColor(ContextCompat.getColor(this, R.color.red20))