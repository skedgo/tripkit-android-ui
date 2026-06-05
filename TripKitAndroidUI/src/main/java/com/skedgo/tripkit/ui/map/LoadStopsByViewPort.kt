package com.skedgo.tripkit.ui.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
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
                    .flatMap { (region, cellIds) ->
                        // Some regions can return/retain stop groups keyed by region name
                        // instead of numeric grid cell ids. Query both to avoid missing
                        // freshly persisted level-tagged rows.
                        val queryCellIds = buildQueryCellIds(cellIds, region.name)
                        // For Room-based approach, we create a selection string that includes
                        // cell codes and bounds, which ScheduledStopRepository will parse
                        val selection = createRoomSelection(queryCellIds.size)
                        val selectionArgs = createRoomSelectionArgs(queryCellIds, bounds)
                        
                        scheduledStopRepository.queryStops(
                            null, // projection not needed for Room
                            selection,
                            selectionArgs,
                            null
                        ).repeatWhen { scheduledStopRepository.changes }
                    }
                    .defaultIfEmpty(emptyList())
            }
            is ViewPort.NotCloseEnough -> Observable.just(emptyList())
            else -> Observable.just(emptyList())
        }
    }
    
    /**
     * Creates a selection string compatible with Room-based ScheduledStopRepository
     * The repository will parse this to extract cell codes and bounds
     */
    private fun createRoomSelection(cellIdsSize: Int): String {
        // This selection string will be parsed by ScheduledStopRepository.extractCellCodesFromSelection
        // and extractBoundsFromSelection methods to extract the necessary information for Room queries
        return "cell_code IN (${"?" + ",?".repeat(cellIdsSize - 1)}) AND lat >= ? AND lat <= ? AND lon >= ? AND lon <= ?"
    }
    
    /**
     * Creates selection arguments compatible with Room-based ScheduledStopRepository
     * Format: [cellCode1, cellCode2, ..., southWestLat, northEastLat, southWestLon, northEastLon]
     */
    private fun createRoomSelectionArgs(cellIds: List<String>, bounds: LatLngBounds): Array<String> {
        val fromLng = minOf(bounds.southwest.longitude, bounds.northeast.longitude)
        val toLng = maxOf(bounds.southwest.longitude, bounds.northeast.longitude)
        
        val selectionArgs = Array(cellIds.size + 4) { "" }
        
        // Add cell codes first
        cellIds.forEachIndexed { index, cellId ->
            selectionArgs[index] = cellId
        }
        
        // Add bounds: lat >= ?, lat <= ?, lon >= ?, lon <= ?
        selectionArgs[cellIds.size] = bounds.southwest.latitude.toString()
        selectionArgs[cellIds.size + 1] = bounds.northeast.latitude.toString()
        selectionArgs[cellIds.size + 2] = fromLng.toString()
        selectionArgs[cellIds.size + 3] = toLng.toString()
        
        return selectionArgs
    }

}

internal fun buildQueryCellIds(cellIds: List<String>, regionName: String?): List<String> {
    val regionKey = regionName?.takeIf { it.isNotBlank() } ?: return cellIds
    return (cellIds + regionKey).distinct()
}