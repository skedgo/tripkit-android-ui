package com.skedgo.tripkit.ui.realtime

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.agenda.IRealTimeElement
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.routing.RealTimeVehicle
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class RealTimeChoreographerViewModel @Inject constructor(
    private val realTimeChoreographer: RealTimeChoreographer
) : ViewModel() {

    private val realTimeVehiclesPublisher = PublishRelay.create<List<RealTimeVehicle>>()

    fun getRealTimeVehicles(
        region: Region,
        services: List<IRealTimeElement>
    ): Observable<List<RealTimeVehicle>> =
        realTimeChoreographer.getMapRealTimeResults(region, services)
            .doOnNext {
                realTimeVehiclesPublisher.accept(it)
            }

    fun realTimeVehicleObservable(service: IRealTimeElement): Observable<RealTimeVehicle> =
        realTimeVehiclesPublisher.hide()
            .observeOn(Schedulers.io())
            .flatMapIterable { vehicles ->
                listOfNotNull(vehicles.find { it.serviceTripId == service.serviceTripId })
            }
}