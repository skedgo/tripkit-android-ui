package com.skedgo.tripkit.ui.routing
import io.reactivex.Completable
import io.reactivex.Observable
import org.joda.time.Minutes

interface PreferredTransferTimeRepository {
  fun putPreferredTransferTime(preferredTransferTime: Minutes): Completable
  fun getPreferredTransferTime(
      defaultIfEmpty: () -> Minutes = { Minutes.THREE }
  ): Observable<Minutes>

  /**
   * Emits when preferred transfer time has changed.
   */
  fun whenPreferredTransferTimeChanges(): Observable<Minutes>
}