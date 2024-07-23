package com.skedgo.tripkit.ui.favorites.trips

import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint

data class FavoriteTrip(
    val uuid: String,
    val fromAddress: String?,
    val toAddress: String?,
    val waypoints: List<Waypoint>,
    var order: Int = 0,
    val tripGroupId: String? = null
)