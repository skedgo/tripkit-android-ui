package com.skedgo.tripkit.ui.database.scheduled_stops

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_stops_download_history",
    indices = [
        Index(value = ["cellCode"], unique = true)
    ]
)
data class ScheduledStopDownloadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cellCode: String,
    val downloadTime: Long,
    val hashCode2: Long
)
