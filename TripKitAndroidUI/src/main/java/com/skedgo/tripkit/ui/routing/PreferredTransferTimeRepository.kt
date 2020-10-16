package com.skedgo.tripkit.ui.routing

import java.time.Duration

interface PreferredTransferTimeRepository {
  suspend fun putPreferredTransferTime(preferredTransferTime: Duration)
  suspend fun getPreferredTransferTime(
      defaultIfEmpty: () -> Duration = { Duration.ofMinutes(3) }
  ): Duration
}