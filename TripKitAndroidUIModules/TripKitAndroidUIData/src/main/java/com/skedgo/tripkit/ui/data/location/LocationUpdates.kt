package com.skedgo.tripkit.ui.data.location

import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult

sealed class LocationUpdates {
    data class Availability(val value: LocationAvailability) : LocationUpdates()
    data class Result(val value: LocationResult) : LocationUpdates()
}