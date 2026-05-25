package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.text.format.DateFormat
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

open class GetRealtimeText @Inject constructor(
    private val context: Context,
    private val printTime: PrintTime
) {

    open fun execute(
        dateTimeZone: DateTimeZone,
        service: TimetableEntry,
        vehicle: RealTimeVehicle? = null
    ): Pair<String, Int> {

        val isRightToLeft = context.resources.getBoolean(R.bool.is_right_to_left)

        // Use system-aware time format instead of hardcoded "HH:mm"
        val timePattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm a"
        val dateTimeFormatter = DateTimeFormat.forPattern(timePattern)
        val startTime = realTimeDeparture(service, service.realtimeVehicle)
        val endTime = realTimeArrival(service, service.realtimeVehicle)
        val startDateTime = DateTime(TimeUnit.SECONDS.toMillis(startTime))
        val endDateTime = DateTime(TimeUnit.SECONDS.toMillis(endTime))
        val schedule = if (endTime > 0) {
            //Just reversing the start time - end time if it's right to left since changing
            // text direction of textview to rtl still showing error
            if (isRightToLeft) {
                "${endDateTime.toString(dateTimeFormatter.withZone(dateTimeZone))} - ${
                    startDateTime.toString(
                        dateTimeFormatter.withZone(dateTimeZone)
                    )
                }"
            } else {
                "${startDateTime.toString(dateTimeFormatter.withZone(dateTimeZone))} - ${
                    endDateTime.toString(
                        dateTimeFormatter.withZone(dateTimeZone)
                    )
                }"
            }
        } else {
            startDateTime.toString(dateTimeFormatter.withZone(dateTimeZone))
        }

        return when {
            service.realTimeStatus == RealTimeStatus.CANCELLED || service.isCancelled ->
                context.getString(R.string.cancelled).uppercase() to R.color.tripKitError
            service.realTimeStatus == null || service.realTimeStatus == RealTimeStatus.INCAPABLE ->
                "${context.getString(R.string.scheduled)} • $schedule" to R.color.black1

            service.realTimeStatus == RealTimeStatus.CAPABLE &&
                realTimeDeparture(service, vehicle) > TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) ->
                "${context.getString(R.string.scheduled)} • $schedule" to R.color.black1
            else -> {

                val serviceTime =
                    printTime.print(DateTime(service.serviceTime * 1000, dateTimeZone))
                val realtimeDeparture = realTimeDeparture(service, vehicle)

                val realtimeDepartureHM = truncateToHourAndMinute(realtimeDeparture)
                val serviceTimeHM = truncateToHourAndMinute(service.serviceTime)
                val endTimeHM = truncateToHourAndMinute(endTime)

                var timeDiff = service.serviceTime - realtimeDeparture
                var isSameHourAndMinutes = serviceTimeHM.first == realtimeDepartureHM.first &&
                    serviceTimeHM.second == realtimeDepartureHM.second
                if (timeDiff > 36000) {
                    timeDiff = endTime - realtimeDeparture
                    isSameHourAndMinutes = endTimeHM.first == realtimeDepartureHM.first &&
                        endTimeHM.second == realtimeDepartureHM.second
                }

                val (status, delayed) = when {
                    abs(timeDiff.toInt()) < 60 && isSameHourAndMinutes ->
                        context.getString(R.string.on_time) to R.color.tripKitSuccess

                    realtimeDeparture < service.serviceTime ->
                        context.getString(
                            R.string.realtime_early,
                            TimeUtils.getDurationInHoursMins(context, abs(timeDiff.toInt()))
                        ) to R.color.tripKitWarning

                    else -> context.getString(
                        R.string.realtime_late,
                        TimeUtils.getDurationInHoursMins(context, abs(timeDiff.toInt()))
                    ) to R.color.tripKitError
                }

                /*"$status • $serviceTime" to delayed*/
                "$status • $schedule" to delayed
            }
        }
    }

    open fun getWithIsOnTime(
        dateTimeZone: DateTimeZone,
        service: TimetableEntry,
        vehicle: RealTimeVehicle? = null
    ): Triple<String, Int, Boolean> {

        val isRightToLeft = context.resources.getBoolean(R.bool.is_right_to_left)

        // Use system-aware time format instead of hardcoded "HH:mm"
        val timePattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm a"
        val dateTimeFormatter = DateTimeFormat.forPattern(timePattern)
        val startTime = realTimeDeparture(service, service.realtimeVehicle)
        val endTime = realTimeArrival(service, service.realtimeVehicle)
        val startDateTime = DateTime(TimeUnit.SECONDS.toMillis(startTime))
        val endDateTime = DateTime(TimeUnit.SECONDS.toMillis(endTime))
        val schedule = if (endTime > 0) {
            //Just reversing the start time - end time if it's right to left since changing
            // text direction of textview to rtl still showing error
            if (isRightToLeft) {
                "${endDateTime.toString(dateTimeFormatter.withZone(dateTimeZone))} - ${
                    startDateTime.toString(
                        dateTimeFormatter.withZone(dateTimeZone)
                    )
                }"
            } else {
                "${startDateTime.toString(dateTimeFormatter.withZone(dateTimeZone))} - ${
                    endDateTime.toString(
                        dateTimeFormatter.withZone(dateTimeZone)
                    )
                }"
            }
        } else {
            startDateTime.toString(dateTimeFormatter.withZone(dateTimeZone))
        }

        return when {
            service.realTimeStatus == RealTimeStatus.CANCELLED || service.isCancelled ->
                Triple(context.getString(R.string.cancelled).uppercase(), R.color.tripKitError, false)
            service.realTimeStatus == null || service.realTimeStatus == RealTimeStatus.INCAPABLE ->
                Triple("${context.getString(R.string.scheduled)} • $schedule", R.color.black1, false)

            service.realTimeStatus == RealTimeStatus.CAPABLE &&
                realTimeDeparture(service, vehicle) > TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) ->
                Triple("${context.getString(R.string.scheduled)} • $schedule", R.color.black1, false)
            else -> {

                val serviceTime =
                    printTime.print(DateTime(service.serviceTime * 1000, dateTimeZone))
                val realtimeDeparture = realTimeDeparture(service, vehicle)

                val realtimeDepartureHM = truncateToHourAndMinute(realtimeDeparture)
                val serviceTimeHM = truncateToHourAndMinute(service.serviceTime)
                val endTimeHM = truncateToHourAndMinute(endTime)

                var timeDiff = service.serviceTime - realtimeDeparture
                var isSameHourAndMinutes = serviceTimeHM.first == realtimeDepartureHM.first &&
                    serviceTimeHM.second == realtimeDepartureHM.second
                if (timeDiff > 36000) {
                    timeDiff = endTime - realtimeDeparture
                    isSameHourAndMinutes = endTimeHM.first == realtimeDepartureHM.first &&
                        endTimeHM.second == realtimeDepartureHM.second
                }

                val (status, delayed) = when {
                    abs(timeDiff.toInt()) < 60 && isSameHourAndMinutes ->
                        Triple(context.getString(R.string.on_time), R.color.tripKitSuccess, true)

                    realtimeDeparture < service.serviceTime ->
                        Triple(context.getString(
                            R.string.realtime_early,
                            TimeUtils.getDurationInHoursMins(context, abs(timeDiff.toInt()))
                        ), R.color.tripKitWarning, false)

                    else -> Triple(
                        context.getString(
                            R.string.realtime_late,
                            TimeUtils.getDurationInHoursMins(context, abs(timeDiff.toInt()))
                        ), R.color.tripKitError, false
                    )
                }

                /*"$status • $serviceTime" to delayed*/
                Triple("$status • $schedule", delayed, false)
            }
        }
    }

    private fun truncateToHourAndMinute(timeInSeconds: Long): Pair<Long, Long> {
        val hours = timeInSeconds / TimeUtils.InSeconds.HOUR
        val minutes = (timeInSeconds % TimeUtils.InSeconds.HOUR) / TimeUtils.InSeconds.MINUTE
        return Pair(hours, minutes)
    }
}