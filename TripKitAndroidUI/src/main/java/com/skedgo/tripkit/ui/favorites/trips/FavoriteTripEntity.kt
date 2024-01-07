package com.skedgo.tripkit.ui.favorites.trips

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey

@Entity(tableName = "favoriteTrips")
data class FavoriteTripEntity(
    @PrimaryKey var uuid: String,
    var fromAddress: String?,
    var toAddress: String?,
    var order: Int,
    var tripGroupId: String?
)

@Entity(
    tableName = "waypoints",
    foreignKeys = arrayOf(ForeignKey(
        entity = FavoriteTripEntity::class,
        parentColumns = arrayOf("uuid"),
        childColumns = arrayOf("tripId"),
        onDelete = CASCADE,
        onUpdate = CASCADE
    )))
class WaypointEntity(
    var lat: Double,
    var lng: Double,
    var mode: String?,
    var modeTitle: String?,
    var order: Int,
    @ColumnInfo(index = true) var tripId: String) {
  @PrimaryKey(autoGenerate = true)
  var id: Int = 0
}