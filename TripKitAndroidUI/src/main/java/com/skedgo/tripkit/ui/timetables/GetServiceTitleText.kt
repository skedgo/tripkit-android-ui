package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.model.TimetableEntry
import org.joda.time.DateTimeZone
import javax.inject.Inject

open class GetServiceTitleText @Inject constructor(
    private val getFrequencyText: GetFrequencyText,
    private val getA2BTime: GetA2BTime,
    private val getOrdinaryTime: GetOrdinaryTime
) {

    open fun execute(
        service: TimetableEntry,
        dateTimeZone: DateTimeZone,
        vehicle: RealTimeVehicle? = null
    ): String {
        val startTimeInSecs = realTimeDeparture(service, vehicle)
        val endTimeInSecs = realTimeArrival(service, vehicle)
        return when {
            service.isFrequencyBased -> getFrequencyText.execute(service)
            endTimeInSecs != 0L -> getA2BTime.execute(
                dateTimeZone,
                service,
                startTimeInSecs,
                endTimeInSecs
            )
            else -> getOrdinaryTime.execute(dateTimeZone, service, vehicle)
        }
    }
}