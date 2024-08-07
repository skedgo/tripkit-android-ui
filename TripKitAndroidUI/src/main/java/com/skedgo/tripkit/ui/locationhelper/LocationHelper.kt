package com.skedgo.tripkit.ui.locationhelper

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.AndroidGeocoder
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * A simple wrapper around [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient).
 *
 * It is your responsibility to make sure that your app has requested the correct permissions to get location.
 *
 */
class LocationHelper constructor(private var context: Context) {
    /**
     * Interface definition for a callback that will be called when a location is either found, or an error occurs.
     */
    interface OnLocationFoundListener {
        /**
         * Called when a location is found.
         *
         * @param location The user's current location.
         */
        fun locationFound(location: Location)

        /**
         * Called when an error occurred.
         *
         * @param error An error string.
         */
        fun locationError(error: String)
    }

    private var listener: OnLocationFoundListener? = null

    /**
     * Retrieves the current location.
     *
     * @param listener The callback to be called when a location is found.
     */
    fun getCurrentLocation(listener: OnLocationFoundListener) {
        this.listener = listener
        getLocation()
    }

    /**
     * Retrieves the current location.
     *
     * @param listener A function to be called when a location is found.
     * @param error A function to be called when an error occurred.
     */
    fun getCurrentLocation(listener: (Location) -> Unit, error: (String) -> Unit) {
        this.listener = object : OnLocationFoundListener {
            override fun locationError(error: String) {
                error(error)
            }

            override fun locationFound(location: Location) {
                listener(location)
            }
        }
        getLocation()
    }

    private val client = FusedLocationProviderClient(context)
    private fun getLocation() {
        client.lastLocation.addOnCompleteListener {
            if (it.isSuccessful) {
                it.result?.let { location ->
                    geocode(location.latitude, location.longitude)
                } ?: this.listener?.locationError("Invalid location")
            } else {
                this.listener?.locationError(it.exception.toString())
            }
        }
    }

    private fun geocode(lat: Double, lon: Double) {
        var geocoder = AndroidGeocoder(context)
        geocoder.getAddress(lat, lon)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                var location = Location()
                location.lat = lat
                location.lon = lon
                location.address = it
                this.listener?.locationFound(location)
            }, { this.listener?.locationError(it.localizedMessage) })

    }
}