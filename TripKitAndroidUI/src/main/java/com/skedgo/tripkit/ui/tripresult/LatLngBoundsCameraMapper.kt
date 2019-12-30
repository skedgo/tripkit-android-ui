package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds

fun LatLngBounds.toCameraUpdate(padding: Int): CameraUpdate
        = CameraUpdateFactory.newLatLngBounds(this, padding)
