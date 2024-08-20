package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

fun realTimeArrival(service: TimetableEntry, vehicle: RealTimeVehicle? = null): Long {
    return listOf(
        vehicle?.arriveAtEndStopTime,
        service.realTimeArrival.toLong(),
        service.endTimeInSecs
    )
        .firstOrNull { it != null && it > 0L } ?: 0
}

fun realTimeDeparture(service: TimetableEntry, vehicle: RealTimeVehicle? = null): Long {
    return listOf(
        vehicle?.arriveAtStartStopTime,
        service.realTimeDeparture.toLong(),
        service.startTimeInSecs
    )
        .firstOrNull { it != null && it > 0L } ?: 0
}

fun TimetableEntry.getTimeLeftToDepartInterval(period: Long, timeUnit: TimeUnit): Observable<Long> {
    return Observable.interval(0, period, timeUnit)
        .map { realTimeDeparture(this, this.realtimeVehicle) }
        .map {
            val secsToDepart = TimeUnit.SECONDS.toMillis(it) - System.currentTimeMillis()
            TimeUnit.MILLISECONDS.toMinutes(secsToDepart)
        }
}