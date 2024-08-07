package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.datetime.PrintTime
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import javax.inject.Inject

open class GetTimeRangeText @Inject constructor(
    private val printTime: PrintTime
) {

    open fun execute(
        dateTimeZone: DateTimeZone,
        startTimeInSecs: Long,
        endTimeInSecs: Long
    ): String {
        val start = printTime.print(DateTime(startTimeInSecs * 1000, dateTimeZone))
        val end = printTime.print(DateTime(endTimeInSecs * 1000, dateTimeZone))
        return "$start - $end"
    }
}