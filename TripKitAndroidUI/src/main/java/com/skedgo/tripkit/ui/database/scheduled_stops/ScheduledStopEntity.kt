package com.skedgo.tripkit.ui.database.scheduled_stops

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_stops",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["cellCode"]),
        Index(value = ["parentId"])
    ]
)
data class ScheduledStopEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val cellCode: String,
    val stopType: String?,
    val shortName: String?,
    val services: String?,
    val parentId: String?,
    val isParent: Int,
    val apiZoomLevel: Int,
    val modeInfo: String?,
    val filter: String?
)
