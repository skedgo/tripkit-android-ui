package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.ui.utils.ignoreNetworkErrors
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

open class FetchStopsByViewport @Inject constructor(
    private val getCellIdsFromViewPort: GetCellIdsFromViewPort,
    private val regionService: RegionService,
    private val stopsFetcher: StopsFetcher
) {

    enum class ClearDataType {
        CAR_PODS
    }

    open fun execute(viewPort: ViewPort): Completable =
        when (viewPort) {
            is ViewPort.CloseEnough -> {
                regionService.getRegionByLocationAsync(
                    viewPort.visibleBounds.southwest.latitude,
                    viewPort.visibleBounds.southwest.longitude
                )
                    .ignoreOutOfRegionsException()
                    .flatMap { region ->
                        val center = viewPort.visibleBounds.center()
                        val defaultParams = FetchStopParams(
                            StopLoaderArgs.getCellIdsForRegionalLevel(center),
                            region,
                            ApiZoomLevels.REGION
                        )

                        if (viewPort.isInner()) {
                            // Local level (> 15.0f) - load local stops + regional for cities
                            val localParams = getCellIdsFromViewPort.execute(viewPort)
                                .map { cellIds ->
                                    FetchStopParams(
                                        cellIds,
                                        region,
                                        ApiZoomLevels.LOCAL
                                    )
                                }
                            
                            // Start with region level, then add local level
                            localParams.startWith(defaultParams)
                        } else {
                            // Regional level (<= 15.0f) - only load regional stops
                            Observable.just(defaultParams)
                        }
                    }
            }
            else -> Observable.empty<FetchStopParams>()
        }.flatMapCompletable {
            stopsFetcher.fetchAsync(it.cellIds, it.region, it.level)
                .ignoreNetworkErrors()
                .ignoreElements()
        }

    open fun fetch(viewPort: ViewPort): Completable {
        return when (viewPort) {
            is ViewPort.CloseEnough -> {
                regionService.getRegionByLocationAsync(
                    viewPort.visibleBounds.southwest.latitude,
                    viewPort.visibleBounds.southwest.longitude
                )
                    .ignoreOutOfRegionsException()
                    .flatMap { region ->
                        val center = viewPort.visibleBounds.center()
                        val defaultParams = FetchStopParams(
                            StopLoaderArgs.getCellIdsForRegionalLevel(center),
                            region,
                            ApiZoomLevels.REGION
                        )

                        if (viewPort.isInner()) {
                            // Local level (> 15.0f) - load local stops + regional for cities
                            val localParams = getCellIdsFromViewPort.fetch(viewPort)
                                .map { cellIds ->
                                    FetchStopParams(
                                        cellIds,
                                        region,
                                        ApiZoomLevels.LOCAL
                                    )
                                }
                            
                            // Start with region level, then add local level
                            localParams.startWith(defaultParams)
                        } else {
                            // Regional level (<= 15.0f) - only load regional stops
                            Observable.just(defaultParams)
                        }
                    }
            }
            else -> Observable.empty()
        }.flatMapCompletable { params ->
            stopsFetcher.fetchAsync(params.cellIds, params.region, params.level)
                .ignoreNetworkErrors()
                .ignoreElements()
        }
    }

    open fun clearData(type: ClearDataType): Completable? {
        return if (type == ClearDataType.CAR_PODS) {
            stopsFetcher.clearCarPods()
        } else {
            null
        }
    }

}


class FetchStopParams(val cellIds: List<String>, val region: Region, val level: Int)

private fun com.skedgo.tripkit.ui.data.places.LatLngBounds.center(): GeoPoint {
    val lat = (southwest.latitude + northeast.latitude) / 2
    val lng = (southwest.longitude + northeast.longitude) / 2
    return GeoPoint(lat, lng)
}