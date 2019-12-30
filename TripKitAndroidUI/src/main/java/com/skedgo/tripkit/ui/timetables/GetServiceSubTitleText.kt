package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.ui.model.TimetableEntry
import org.joda.time.DateTimeZone
import javax.inject.Inject

// TODO Can delete?
open class GetServiceSubTitleText @Inject constructor(
        private val getTimeRangeText: GetTimeRangeText,
        private val getDirectionText: GetDirectionText
) {

  open fun execute(service: TimetableEntry, dateTimeZone: DateTimeZone): String {
    val startTime = realTimeDeparture(service, service.realtimeVehicle)
    val endTime = realTimeArrival(service, service.realtimeVehicle)
    return when {
      service.isFrequencyBased -> getTimeRangeText.execute(dateTimeZone, startTime, endTime)
      else -> getDirectionText.execute(service)
    }
  }
}