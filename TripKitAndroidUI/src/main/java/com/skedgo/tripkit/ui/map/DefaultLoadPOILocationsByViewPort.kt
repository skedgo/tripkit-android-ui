package com.skedgo.tripkit.ui.map
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable
import javax.inject.Inject

class DefaultLoadPOILocationsByViewPort @Inject constructor(
        private val stopInfoWindowAdapter: StopInfoWindowAdapter,
        private val loadStopsByViewPort: LoadStopsByViewPort,
        private val loadBikePodsByViewPort: LoadBikePodsByViewPort,
        private val loadCarPodByViewPort: LoadCarPodByViewPort) : LoadPOILocationsByViewPort {

  override fun execute(viewPort: ViewPort): Observable<List<POILocation>> {
    return Observable
        .just(loadBikePodsByViewPort.execute(viewPort).map { it.map { BikePodPOILocation(it) } },
            loadStopsByViewPort.execute(viewPort).map { it.map { StopPOILocation(it, stopInfoWindowAdapter) } },
            loadCarPodByViewPort.execute(viewPort).map { it.map { CarPodPOILocation(it) } })
        .toList()
            .toObservable()
        .flatMap {
          Observable.combineLatest(it)
          { (a, b, c) ->
            a as List<POILocation> + b as List<POILocation> + c as List<POILocation>
          }
        }
        .defaultIfEmpty(emptyList())
  }
}