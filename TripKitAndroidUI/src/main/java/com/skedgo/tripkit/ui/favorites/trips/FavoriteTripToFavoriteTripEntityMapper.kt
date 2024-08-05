package com.skedgo.tripkit.ui.favorites.trips

import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint


internal fun FavoriteTrip.toFavoriteTripEntity(): FavoriteTripEntity =
    FavoriteTripEntity(uuid, fromAddress, toAddress, order, tripGroupId)

internal fun FavoriteTripEntity.toFavoriteTrip(waypoints: List<Waypoint>): FavoriteTrip =
    FavoriteTrip(uuid, fromAddress, toAddress, waypoints, order, this.tripGroupId)