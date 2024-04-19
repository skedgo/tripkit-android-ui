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
    private val loadCarParksByViewPort: LoadCarParksByViewPort
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
                loadCarPodByViewPort.execute(viewPort)
                    .map { it.map { CarPodPOILocation(it) } },
                loadFacilitiesByViewPort.execute(viewPort)
                    .map { it.map { FacilityPOILocation(it) } },
                loadCarParksByViewPort.execute(viewPort)
                    .map { it.map { CarParkPOILocation(it) } },
            )
            .toList()
            .toObservable()
            .flatMap { items ->
                Observable.combineLatest(items) { results ->
                    val bikePods = results[0] as List<IMapPoiLocation>
                    val freeFloatingVehicles = results[1] as List<IMapPoiLocation>
                    val stops = results[2] as List<IMapPoiLocation>
                    val carPods = results[3] as List<IMapPoiLocation>
                    val facilities = results[4] as List<IMapPoiLocation>
                    val carParks = results[5] as List<IMapPoiLocation>
                    bikePods + freeFloatingVehicles + stops + carPods + facilities + carParks
                }
            }
            .defaultIfEmpty(emptyList())
    }
}