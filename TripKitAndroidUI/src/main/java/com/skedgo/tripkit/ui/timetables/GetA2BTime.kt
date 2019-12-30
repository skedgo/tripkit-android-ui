package com.skedgo.tripkit.ui.timetables

import android.content.Context
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import org.joda.time.DateTimeZone
import javax.inject.Inject

open class GetA2BTime @Inject constructor(
    private val context: Context,
    private val getTimeRangeText: GetTimeRangeText
) {
  open fun execute(dateTimeZone: DateTimeZone, service: TimetableEntry, startTimeInSecs: Long, endTimeInSecs: Long): String {
    val timeRangeText = getTimeRangeText.execute(dateTimeZone, startTimeInSecs, endTimeInSecs)
    val serviceTitle = when {
      service.serviceNumber.isNullOrEmpty().not() -> service.serviceNumber
      service.startStop?.type != null -> service.startStop.type.toString().capitalize()
      else -> context.getString(R.string.service)
    }
    return "$serviceTitle: $timeRangeText"
  }
}