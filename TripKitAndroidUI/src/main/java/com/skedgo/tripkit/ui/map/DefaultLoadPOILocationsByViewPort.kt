package com.skedgo.tripkit.ui.map

import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable
import javax.inject.Inject

class DefaultLoadPOILocationsByViewPort @Inject constructor(
    private val stopInfoWindowAdapter: StopInfoWindowAdapter,
    private val loadStopsByViewPort: LoadStopsByViewPort,
    private val loadBikePodsByViewPort: LoadBikePodsByViewPort,
    private val loadFreeFloatingVehiclesByViewPort: LoadFreeFloatingVehiclesByViewPort,
    private val loadCarPodByViewPort: LoadCarPodByViewPort,
    private val loadFacilitiesByViewPort: LoadFacilitiesByViewPort,
) : LoadPOILocationsByViewPort {

    override fun execute(viewPort: ViewPort): Observable<List<IMapPoiLocation>> {
        return Observable
            .just(
                loadBikePodsByViewPort.execute(viewPort)
                    .map { it.map { BikePodPOILocation(it) } },
                loadFreeFloatingVehiclesByViewPort.execute(viewPort)
                    .map { it.map { FreeFloatingVehiclePOILocation(it) } },
                loadStopsByViewPort.execute(viewPort)
                    .map { it.map { StopPOILocation(it, stopInfoWindowAdapter) } },
                loadCarPodByViewPort.execute(viewPort).map { it.map { CarPodPOILocation(it) } },
                loadFacilitiesByViewPort.execute(viewPort)
                    .map { it.map { FacilityPOILocation(it) } }
            )
            .toList()
            .toObservable()
            .flatMap {
                Observable.combineLatest(it)
                { (bikePods, freeFloatingVehicles, stops, carPods, facilities) ->
                    bikePods as List<IMapPoiLocation> +
                            freeFloatingVehicles as List<IMapPoiLocation> +
                            stops as List<IMapPoiLocation> +
                            carPods as List<IMapPoiLocation> +
                            facilities as List<IMapPoiLocation>
                }
            }
            .defaultIfEmpty(emptyList())
    }
}