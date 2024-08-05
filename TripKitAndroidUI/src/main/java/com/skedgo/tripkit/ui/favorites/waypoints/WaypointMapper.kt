package com.skedgo.tripkit.ui.favorites.waypoints

internal fun WaypointEntity.toWaypoint(): Waypoint =
    Waypoint(lat, lng, mode, mode?.run { listOf(this) }, modeTitle)

internal fun Waypoint.toWaypointEntity(tripId: String, order: Int): WaypointEntity =
    WaypointEntity(
        lat ?: 0.0,
        lng ?: 0.0,
        mode,
        mode?.run { listOf(this) },
        modeTitle,
        order,
        tripId
    )