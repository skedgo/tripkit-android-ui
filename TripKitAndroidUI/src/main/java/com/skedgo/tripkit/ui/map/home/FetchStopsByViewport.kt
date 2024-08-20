package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.data.regions.RegionService
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
                        val parentStops =
                            FetchStopParams(listOf(region.name!!), region, ApiZoomLevels.REGION)
                        if (viewPort.isInner()) {
                            getCellIdsFromViewPort.execute(viewPort)
                                .map {
                                    FetchStopParams(
                                        it, region,
                                        ApiZoomLevels.fromMapZoomLevel(ZoomLevel.fromLevel(viewPort.zoom))
                                    )
                                }
                                .startWith(parentStops)
                        } else {
                            Observable.just(parentStops)
                        }
                    }
            }
            else -> Observable.empty<FetchStopParams>()
        }
            .flatMapCompletable {
                stopsFetcher.fetchAsync(it.cellIds, it.region, it.level)
                    .ignoreNetworkErrors()
                    .ignoreElements()
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