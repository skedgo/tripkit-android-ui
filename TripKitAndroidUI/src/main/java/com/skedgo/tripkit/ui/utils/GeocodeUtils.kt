package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Locale

fun Context.getLocationFromAddress(addressString: String): Pair<Double, Double>? {
    val geocoder = Geocoder(
        this,
        Locale.getISOCountries().firstOrNull { it == "AU" }?.let { Locale(it) }
            ?: Locale.getDefault())
    return try {
        val addresses: List<Address> = geocoder.getFromLocationName(addressString, 1).orEmpty()
        if (addresses.isNotEmpty()) {
            val address = addresses[0]
            // Return the latitude and longitude
            Pair(address.latitude, address.longitude)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}