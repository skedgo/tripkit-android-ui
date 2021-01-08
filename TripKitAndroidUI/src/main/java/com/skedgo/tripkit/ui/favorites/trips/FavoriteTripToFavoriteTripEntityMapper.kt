package com.skedgo.tripkit.ui.favorites.trips


internal fun FavoriteTrip.toFavoriteTripEntity(): FavoriteTripEntity =
    FavoriteTripEntity(uuid, fromAddress, toAddress, order, tripGroupId)

internal fun FavoriteTripEntity.toFavoriteTrip(waypoints: List<Waypoint>): FavoriteTrip =
    FavoriteTrip(uuid, fromAddress, toAddress, waypoints, order, this.tripGroupId)

internal fun WaypointEntity.toWaypoint(): Waypoint = Waypoint(lat, lng, mode, modeTitle)

internal fun Waypoint.toWaypointEntity(tripId: String, order: Int): WaypointEntity =
    WaypointEntity(lat, lng, mode, modeTitle, order, tripId)