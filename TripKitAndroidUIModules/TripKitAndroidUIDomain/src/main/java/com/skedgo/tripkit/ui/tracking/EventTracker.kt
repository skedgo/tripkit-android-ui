package com.skedgo.tripkit.ui.tracking
interface EventTracker {
  fun log(event: Event)

  /**
   * return true if this tracker tracks user data.
   */
  fun doesTrackUserData(): Boolean
}