package com.skedgo.tripkit.ui.database.scheduled_stops

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduledStopWithLocation(
    @Embedded
    val scheduledStop: ScheduledStopEntity,
    
    @Relation(
        parentColumn = "code",
        entityColumn = "scheduledStopCode"
    )
    val location: LocationEntity?,
    
    @Relation(
        parentColumn = "cellCode",
        entityColumn = "cellCode"
    )
    val downloadHistory: ScheduledStopDownloadHistoryEntity? = null
)
