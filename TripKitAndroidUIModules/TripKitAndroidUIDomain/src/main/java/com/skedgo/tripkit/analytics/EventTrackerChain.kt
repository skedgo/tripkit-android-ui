package com.skedgo.tripkit.analytics
import com.skedgo.tripkit.ui.tracking.Event
import com.skedgo.tripkit.ui.tracking.EventTracker
import timber.log.Timber
import javax.inject.Inject

class EventTrackerChain @Inject constructor(private val factory: EvenTrackerFactory) : EventTracker {
  val trackers = factory.create()

  override fun log(event: Event) {
    trackers.forEach {
      it.log(event)
    }
    Timber.d("Log: $event")
  }

  override fun doesTrackUserData(): Boolean = trackers.any { it.doesTrackUserData() }
}