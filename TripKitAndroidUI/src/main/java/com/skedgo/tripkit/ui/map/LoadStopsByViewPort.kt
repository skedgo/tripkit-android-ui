package com.skedgo.tripkit.ui.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.data.CursorToStopConverter
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.StopLoaderArgs
import com.skedgo.tripkit.ui.map.home.ViewPort
import com.skedgo.tripkit.ui.map.home.ignoreOutOfRegionsException
import io.reactivex.Observable
import javax.inject.Inject

open class LoadStopsByViewPort @Inject constructor(
    private val getCellIdsFromViewPort: GetCellIdsFromViewPort,
    private val scheduledStopRepository: ScheduledStopRepository,
    private val regionService: RegionService
) {

    open fun execute(viewPort: ViewPort): Observable<List<ScheduledStop>> {
        return when (viewPort) {
            is ViewPort.CloseEnough -> {
                val bounds = LatLngBounds.builder()
                    .include(
                        LatLng(
                            viewPort.visibleBounds.southwest.latitude,
                            viewPort.visibleBounds.southwest.longitude
                        )
                    )
                    .include(
                        LatLng(
                            viewPort.visibleBounds.northeast.latitude,
                            viewPort.visibleBounds.northeast.longitude
                        )
                    )
                    .build()
                regionService.getRegionByLocationAsync(
                    bounds.center.latitude,
                    bounds.center.longitude
                )
                    .ignoreOutOfRegionsException()
                    .flatMap { region ->
                        getCellIdsFromViewPort.execute(viewPort)
                            .map { region to it }
                    }
                    .map { (region, cellIds) ->
                        StopLoaderArgs.newArgsForStopsLoader(cellIds, region, bounds)
                    }
                    .map { it.first to it.second }
                    .flatMap { (cellIds, bounds) ->
                        val selectionArgs =
                            StopLoaderArgs.createStopLoaderSelectionArgs(cellIds, bounds)
                        val selection = StopLoaderArgs.createStopLoaderSelection(cellIds.size)
                        scheduledStopRepository.queryStops(
                            CursorToStopConverter.PROJECTION,
                            selection,
                            selectionArgs,
                            null
                        )
                            .repeatWhen { scheduledStopRepository.changes }
                    }
                    .defaultIfEmpty(emptyList())
            }
            is ViewPort.NotCloseEnough -> Observable.just(emptyList())
            else -> Observable.just(emptyList())
        }
    }
}