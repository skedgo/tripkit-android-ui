package com.skedgo.tripkit.ui.favorites.trips

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoriteTrips")
data class FavoriteTripEntity(
    @PrimaryKey var uuid: String,
    var fromAddress: String?,
    var toAddress: String?,
    var order: Int,
    var tripGroupId: String?
)