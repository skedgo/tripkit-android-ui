package com.skedgo.tripkit.ui.map

import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable
import com.skedgo.tripkit.parkingspots.ParkingRepository
import com.skedgo.tripkit.parkingspots.models.OffStreetParking
import javax.inject.Inject

open class LoadCarParksByViewPort @Inject constructor(
    private val parkingRepository: ParkingRepository,
    private val getCellIdsFromViewPort: GetCellIdsFromViewPort
) {

    open fun execute(viewPort: ViewPort): Observable<List<OffStreetParking>> {
        return when (viewPort) {
            is ViewPort.CloseEnough -> {
                getCellIdsFromViewPort.execute(viewPort)
                    .flatMap {
                        parkingRepository.getByCellIds(it)
                    }
                    .defaultIfEmpty(emptyList())
            }

            else -> Observable.just(emptyList())
        }
    }
}
