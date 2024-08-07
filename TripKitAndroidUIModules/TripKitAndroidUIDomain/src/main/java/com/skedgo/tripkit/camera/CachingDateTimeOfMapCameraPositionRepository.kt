package com.skedgo.tripkit.camera

import io.reactivex.Observable
import org.joda.time.DateTime

interface CachingDateTimeOfMapCameraPositionRepository {
    fun putCachingDateTime(dateTime: DateTime): Observable<Unit>
    fun getCachingDateTime(): Observable<DateTime>
}