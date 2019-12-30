package com.skedgo.tripkit.camera

import org.joda.time.DateTime
import io.reactivex.Observable

interface CachingDateTimeOfMapCameraPositionRepository {
  fun putCachingDateTime(dateTime: DateTime): Observable<Unit>
  fun getCachingDateTime(): Observable<DateTime>
}