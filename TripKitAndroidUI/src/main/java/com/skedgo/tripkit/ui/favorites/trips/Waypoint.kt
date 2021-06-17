package com.skedgo.tripkit.ui.favorites.trips

data class Waypoint(
        val lat: Double? = 0.0,
        val lng: Double? = 0.0,
        val mode: String?,
        val modeTitle: String?,
        val time: Long = 0,
        val start: String? = null,
        val end: String? = null,
        val startTime: String? = null,
        val endTime: String? = null,
        val serviceTripId: String? = null,
        val operator: String? = null,
        val region: String? = null,
        val disembarkationRegion: String? = null)