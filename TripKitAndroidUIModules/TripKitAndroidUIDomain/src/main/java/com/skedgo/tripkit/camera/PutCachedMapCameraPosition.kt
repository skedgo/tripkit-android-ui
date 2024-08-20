package com.skedgo.tripkit.camera

import io.reactivex.Observable
import javax.inject.Inject

open class PutCachedMapCameraPosition @Inject constructor(
    private val cachingDateTimeOfMapCameraPositionRepository: CachingDateTimeOfMapCameraPositionRepository,
    private val lastCameraPositionRepository: LastCameraPositionRepository
) {
    open fun execute(cachedMapCameraPosition: CachedMapCameraPosition): Observable<Unit> =
        cachingDateTimeOfMapCameraPositionRepository.putCachingDateTime(cachedMapCameraPosition.cachingDateTime)
            .flatMap { lastCameraPositionRepository.putMapCameraPosition(cachedMapCameraPosition.mapCameraPosition) }
            .map { Unit }
}