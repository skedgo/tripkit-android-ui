package com.skedgo.tripkit.ui.database.location_history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_history")
data class LocationHistoryEntity(
    @PrimaryKey
    val address: String,
    val name: String?,
    val lat: Double,
    val lon: Double,
    val exact: Boolean,
    val bearing: Int,
    val phone: String,
    val url: String,
    val timezone: String?,
    val popularity: Int,
    val locationClass: String?,
    val w3w: String,
    val wewInfoURL: String,
    val createdAt: Long
)