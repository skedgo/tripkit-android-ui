package com.skedgo.tripkit.ui.map

import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingRepository
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable
import javax.inject.Inject

open class LoadFreeFloatingVehiclesByViewPort @Inject constructor(
    private val freeFloatingRepository: FreeFloatingRepository,
    private val getCellIdsFromViewPort: GetCellIdsFromViewPort
) {

    open fun execute(viewPort: ViewPort): Observable<List<FreeFloatingLocationEntity>> {
        return when (viewPort) {
            is ViewPort.CloseEnough -> {
                val southwest = GeoPoint(
                    viewPort.visibleBounds.southwest.latitude,
                    viewPort.visibleBounds.southwest.longitude
                )
                val northeast = GeoPoint(
                    viewPort.visibleBounds.northeast.latitude,
                    viewPort.visibleBounds.northeast.longitude
                )
                getCellIdsFromViewPort.execute(viewPort)
                    .flatMap {
                        freeFloatingRepository.getFreeFloatingLocationsWithinBounds(
                            cellIds = it,
                            southwest = southwest,
                            northEast = northeast
                        )
                    }
                    .defaultIfEmpty(emptyList())
            }
            else -> Observable.just(emptyList())
        }
    }
}