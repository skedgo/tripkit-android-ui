package com.skedgo.tripkit.ui.map

import com.skedgo.tripkit.data.database.locations.facility.FacilityLocationEntity
import com.skedgo.tripkit.data.database.locations.facility.FacilityRepository
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable
import com.skedgo.tripkit.location.GeoPoint
import javax.inject.Inject

open class LoadFacilitiesByViewPort @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val getCellIdsFromViewPort: GetCellIdsFromViewPort
) {

    open fun execute(viewPort: ViewPort): Observable<List<FacilityLocationEntity>> {
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
                        facilityRepository.getFacilitiesWithinBounds(
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