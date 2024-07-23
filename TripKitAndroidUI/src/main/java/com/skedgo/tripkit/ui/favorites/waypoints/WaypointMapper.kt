package com.skedgo.tripkit.ui.favorites.waypoints

internal fun WaypointEntity.toWaypoint(): Waypoint = Waypoint(lat, lng, mode, modeTitle)

internal fun Waypoint.toWaypointEntity(tripId: String, order: Int): WaypointEntity =
    WaypointEntity(lat ?: 0.0, lng ?: 0.0, mode, modeTitle, order, tripId)