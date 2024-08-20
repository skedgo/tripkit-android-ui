package com.skedgo.tripkit.ui.favorites.trips

import com.skedgo.tripkit.routing.Trip

fun Trip.toFavoriteTrip(): FavoriteTrip =
    FavoriteTrip(
        uuid(),
        from.displayName,
        to?.displayName,
        toWaypoints(),
        tripGroupId = group.uuid()
    )