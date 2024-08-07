package com.skedgo.tripkit.camera

import io.reactivex.Observable
import javax.inject.Inject

open class GetInitialMapCameraPosition @Inject constructor(
    private val getCachedMapCameraPosition: GetCachedMapCameraPosition,
    private val getCurrentMapCameraPosition: GetCurrentMapCameraPosition,
    private val isCachedMapCameraPositionStale: IsCachedMapCameraPositionStale,
    private val lastCameraPositionRepository: LastCameraPositionRepository
) {
    open fun execute(): Observable<MapCameraPosition> {
        return if (lastCameraPositionRepository.hasMapCameraPosition()) {
            getCachedMapCameraPosition.execute()
                .switchMap {
                    if (isCachedMapCameraPositionStale.execute(it.cachingDateTime)) {
                        Observable.just(it.mapCameraPosition)
                            .concatWith(getCurrentMapCameraPosition.execute())
                    } else {
                        Observable.just(it.mapCameraPosition)
                    }
                }
        } else {
            lastCameraPositionRepository.getDefaultMapCameraPosition()
                .concatWith(getCurrentMapCameraPosition.execute())
        }
    }
}
