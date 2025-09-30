package com.skedgo.tripkit.ui.utils

import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.MarkerManager.Collection

// TripKitFragment
const val MARKER_COLLECTION_CURRENT_LOCATION = "CurrentLocationMarkers"
const val MARKER_COLLECTION_DEPARTURE = "DepartureMarkers"
const val MARKER_COLLECTION_ARRIVAL = "ArrivalMarkers"
const val MARKER_COLLECTION_TRIP_LOCATION = "TripLocationMarkers"
const val MARKER_COLLECTION_CITY = "CityMarkers"
const val MARKER_COLLECTION_POI = "poiMarkers"

// TripResultMapContributor
const val MARKER_COLLECTION_TRAVELLED_STOP = "travelledStopMarkers"
const val MARKER_COLLECTION_VEHICLE = "vehicleMarkers"
const val MARKER_COLLECTION_SEGMENT = "segmentMarkers"
const val MARKER_COLLECTION_NON_TRAVELLED_STOP = "nonTravelledStopMarkers"
const val MARKER_COLLECTION_ALERT = "alertMarkers"

fun MarkerManager.getOrNewCollection(name: String): Collection =
    getCollection(name) ?: newCollection(name)