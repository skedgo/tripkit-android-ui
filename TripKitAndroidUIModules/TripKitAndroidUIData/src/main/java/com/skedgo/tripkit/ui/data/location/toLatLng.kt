package com.skedgo.tripkit.ui.data.location
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.common.model.Location

fun Location.toLatLng(): LatLng = LatLng(lat, lon)
