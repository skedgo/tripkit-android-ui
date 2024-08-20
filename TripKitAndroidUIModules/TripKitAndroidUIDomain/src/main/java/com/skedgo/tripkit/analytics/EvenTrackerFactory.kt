package com.skedgo.tripkit.analytics

import com.skedgo.tripkit.ui.tracking.EventTracker
import javax.inject.Inject

class EvenTrackerFactory @Inject internal constructor() {
    fun create(): List<EventTracker> = emptyList()
}