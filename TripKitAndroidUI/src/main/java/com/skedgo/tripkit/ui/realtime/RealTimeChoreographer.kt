package com.skedgo.tripkit.ui.realtime

import com.skedgo.tripkit.common.agenda.IRealTimeElement
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.routing.RealTimeVehicle
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val UPDATE_INTERVAL = TimeUtils.InMillis.SECOND * 15

open class RealTimeChoreographer @Inject constructor(
    private val realTimeRepository: RealTimeRepository
) {

    open fun getRealTimeResults(
        region: Region,
        elements: List<IRealTimeElement>
    ): Observable<List<RealTimeVehicle>> {
        return realTimeRepository.getUpdates(region.name!!, elements)
            .toObservable()
            .onErrorResumeNext(Observable.empty<List<RealTimeVehicle>>())
            .repeatWhen { observable -> observable.delay(UPDATE_INTERVAL, TimeUnit.MILLISECONDS) }
    }

    open fun getRealTimeResultsFromCleanElements(
        region: Region, elements: List<IRealTimeElement?>
    ): Observable<List<RealTimeVehicle>> {
        return getRealTimeResults(region, cleanSegments(elements))
    }

    private fun cleanSegments(elements: List<IRealTimeElement?>?): List<IRealTimeElement> {
        return elements.orEmpty()
            .filter { it != null }
            .map { it!! }
            .distinctBy { Triple(it.serviceTripId, it.startStopCode, it.endStopCode) }
    }
}