package com.skedgo.tripkit.camera

import io.reactivex.Observable
import io.reactivex.Observable.*
import javax.inject.Inject

open class GetInitialMapCameraPosition @Inject constructor(
        private val getCachedMapCameraPosition: GetCachedMapCameraPosition,
        private val getCurrentMapCameraPosition: GetCurrentMapCameraPosition,
        private val isCachedMapCameraPositionStale: IsCachedMapCameraPositionStale,
        private val lastCameraPositionRepository: LastCameraPositionRepository
) {
  open fun execute(
      requestLocationPermission: () -> Observable<Boolean> = { just(true) }
  ): Observable<MapCameraPosition> {
    fun safelyGetCurrentMapCameraPosition() = defer { requestLocationPermission() }
        .flatMap {
          when (it) {
            true -> getCurrentMapCameraPosition.execute()
            false -> empty()
          }
        }
    return if (lastCameraPositionRepository.hasMapCameraPosition()) {
      getCachedMapCameraPosition.execute()
          .switchMap {
            if (isCachedMapCameraPositionStale.execute(it.cachingDateTime)) {
              Observable.just(it.mapCameraPosition)
                  .concatWith(safelyGetCurrentMapCameraPosition())
            } else {
              Observable.just(it.mapCameraPosition)
            }
          }
    } else {
      lastCameraPositionRepository.getMapCameraPositionByLocale()
          .concatWith(safelyGetCurrentMapCameraPosition())
    }
  }
}
