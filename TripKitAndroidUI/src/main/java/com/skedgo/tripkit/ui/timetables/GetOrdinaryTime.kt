package com.skedgo.tripkit.ui.timetables

import android.content.Context
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import javax.inject.Inject

open class GetOrdinaryTime @Inject constructor(
    private val context: Context,
    private val printTime: PrintTime
) {
    open fun execute(
        dateTimeZone: DateTimeZone,
        service: TimetableEntry,
        vehicle: RealTimeVehicle? = null
    ): String {
        val departureTime =
            printTime.print(DateTime(realTimeDeparture(service, vehicle) * 1000, dateTimeZone))
        val serviceTitle = when {
            service.serviceNumber?.isNotEmpty() ?: false -> service.serviceNumber
            service.startStop?.type != null -> service.startStop.type.toString().capitalize()
            else -> context.getString(R.string.service)
        }
        return context.getString(R.string._pattern_at__pattern, serviceTitle, departureTime)
    }
}