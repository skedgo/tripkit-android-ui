package com.skedgo.tripkit.ui.timetables

import android.content.Context
import com.skedgo.tripkit.common.model.RealTimeStatus
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.RealTimeVehicle
import javax.inject.Inject
import kotlin.math.abs

open class GetRealtimeText @Inject constructor(
    private val context: Context,
    private val printTime: PrintTime
) {

  open fun execute(dateTimeZone: DateTimeZone, service: TimetableEntry, vehicle: RealTimeVehicle? = null): Pair<String, Int> {
    return when {
      service.realTimeStatus == null || service.realTimeStatus == RealTimeStatus.INCAPABLE -> context.getString(R.string.scheduled) to R.color.black1
      service.realTimeStatus == RealTimeStatus.CAPABLE -> context.getString(R.string.no_realtime_available) to R.color.black1
      else -> {
        val serviceTime = printTime.print(DateTime(service.serviceTime * 1000, dateTimeZone))
        val realtimeDeparture = realTimeDeparture(service, vehicle)
        val timeDiff = service.serviceTime - realtimeDeparture

        val (status, delayed) = when {
          abs(timeDiff.toInt()) < 60 -> context.getString(R.string.on_time) to R.color.tripKitSuccess
          realtimeDeparture < service.serviceTime ->
            context.getString(R.string.realtime_early,
                TimeUtils.getDurationInHoursMins(abs(timeDiff.toInt()))) to R.color.tripKitSuccess
          else -> context.getString(R.string.realtime_late,
              TimeUtils.getDurationInHoursMins(abs(timeDiff.toInt()))) to R.color.tripKitError
        }
        "$status • $serviceTime" to delayed
      }
    }
  }
}