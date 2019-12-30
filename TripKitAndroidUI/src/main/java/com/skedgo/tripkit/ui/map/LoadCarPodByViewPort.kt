package com.skedgo.tripkit.ui.map
import com.skedgo.tripkit.data.database.locations.carpods.CarPodRepository
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.locations.CarPod
import javax.inject.Inject

open class LoadCarPodByViewPort @Inject constructor(
        private val carPodRepository: CarPodRepository,
        private val getCellIdsFromViewPort: GetCellIdsFromViewPort) {
  open fun execute(viewPort: ViewPort): Observable<List<CarPod>> {
    return when {
      viewPort.isInner() && viewPort is ViewPort.CloseEnough -> {
        val southwest = GeoPoint(viewPort.visibleBounds.southwest.latitude, viewPort.visibleBounds.southwest.longitude)
        val northeast = GeoPoint(viewPort.visibleBounds.northeast.latitude, viewPort.visibleBounds.northeast.longitude)
        getCellIdsFromViewPort.execute(viewPort)
            .flatMap {
              carPodRepository.getCarPodsByCellIdsWithinBounds(cellIds = it,
                  southwest = southwest,
                  northEast = northeast)
            }
            .defaultIfEmpty(emptyList())
      }
      else -> Observable.just(emptyList())
    }
  }
}