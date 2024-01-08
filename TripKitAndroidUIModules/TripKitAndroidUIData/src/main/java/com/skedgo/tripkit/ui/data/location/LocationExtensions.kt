package com.skedgo.tripkit.ui.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.Locale

fun Context.getLocationName(latLng: LatLng): String? {
    val geocoder = Geocoder(this, Locale.getDefault())

    try {
        val addresses: List<Address> =
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) ?: emptyList()

        if (addresses.isNotEmpty()) {
            val address = addresses[0]
            return address.getAddressLine(0)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return null
}