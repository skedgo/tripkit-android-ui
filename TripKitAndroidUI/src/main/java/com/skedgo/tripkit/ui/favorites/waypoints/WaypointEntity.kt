package com.skedgo.tripkit.ui.favorites.waypoints

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints"
)
class WaypointEntity(
    var lat: Double,
    var lng: Double,
    var mode: String?,
    var modeTitle: String?,
    var order: Int, var tripId: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}