package com.skedgo.tripkit.camera

import org.joda.time.DateTime
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import javax.inject.Inject

open class GetCachedMapCameraPosition @Inject internal constructor(
        private val cachingDateTimeOfMapCameraPositionRepository: CachingDateTimeOfMapCameraPositionRepository,
        private val lastCameraPositionRepository: LastCameraPositionRepository
) {
  open fun execute(): Observable<CachedMapCameraPosition> = cachingDateTimeOfMapCameraPositionRepository
      .getCachingDateTime()
      .switchIfEmpty(Observable.fromCallable { DateTime(0) })
      .zipWith(lastCameraPositionRepository.getMapCameraPosition(),
              BiFunction { time, position -> CachedMapCameraPosition(cachingDateTime = time, mapCameraPosition = position) })
}