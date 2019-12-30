package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.location.GeoPoint

const val CameraUpdatePadding = 120
const val ZoomToCoverFirstOneKilometers = 16.0f
const val ZoomOnSingleLocation = 21.0f
const val OneKilometers = 1000.00

fun List<GeoPoint>.toCameraUpdate(): CameraUpdate = this
        .map { LatLng(it.latitude, it.longitude) }
        .toList()
        .fold(LatLngBounds.Builder(), LatLngBounds.Builder::include)
        .build()
        .toCameraUpdate(CameraUpdatePadding)
