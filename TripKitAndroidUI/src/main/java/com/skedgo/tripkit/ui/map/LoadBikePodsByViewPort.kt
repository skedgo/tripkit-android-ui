package com.skedgo.tripkit.ui.map
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodLocationEntity
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepository
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable
import com.skedgo.tripkit.location.GeoPoint
import javax.inject.Inject

open class LoadBikePodsByViewPort @Inject constructor(
        private val bikePodRepository: BikePodRepository,
        private val getCellIdsFromViewPort: GetCellIdsFromViewPort) {

  open fun execute(viewPort: ViewPort): Observable<List<BikePodLocationEntity>> {
    return when {
      viewPort.isInner() -> {
        val southwest = GeoPoint(viewPort.visibleBounds.southwest.latitude, viewPort.visibleBounds.southwest.longitude)
        val northeast = GeoPoint(viewPort.visibleBounds.northeast.latitude, viewPort.visibleBounds.northeast.longitude)
        getCellIdsFromViewPort.execute(viewPort)
            .flatMap {
              bikePodRepository.getBikePodsWithinBounds(cellIds = it,
                  southwest = southwest,
                  northEast = northeast)
            }
            .defaultIfEmpty(emptyList())
      }
      else -> Observable.just(emptyList())
    }
  }
}