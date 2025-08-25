package com.skedgo.tripkit.ui.database.scheduled_stops

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["scheduledStopCode"]),
        Index(value = ["lat", "lon"]),
        Index(value = ["favouriteSortOrderPosition"])
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String?,
    val address: String?,
    val lat: Double,
    val lon: Double,
    val exact: Int,
    val bearing: Int,
    val favourite: Int,
    val favouriteSortOrderPosition: Int,
    val hasCar: Int,
    val hasMotorbike: Int,
    val hasTaxi: Int,
    val hasBicycle: Int,
    val hasPubTrans: Int,
    val scheduledStopCode: String?,
    val locationType: Int,
    val isDynamic: Int
)
