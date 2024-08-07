package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.analytics.EventTrackerChain
import com.skedgo.tripkit.ui.tracking.EventTracker
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class EventTrackerModule {
    @Provides
    @Singleton
    internal fun eventTracker(eventTrackerChain: EventTrackerChain): EventTracker =
        eventTrackerChain
}