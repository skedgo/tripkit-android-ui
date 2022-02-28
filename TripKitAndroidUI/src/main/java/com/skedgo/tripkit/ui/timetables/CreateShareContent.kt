package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.common.model.RealTimeStatus
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.reactivex.Observable
import javax.inject.Inject

open class CreateShareContent @Inject constructor(private val regionService: RegionService) {
    open fun execute(shareUrl: String, stop: ScheduledStop, services: List<TimetableEntry>): Observable<String> =
            regionService.getRegionByLocationAsync(stop)
                    .map { region ->
                        val rows = StringBuilder()
                        val limit = 10
                        var gotARealTime = false
                        rows.append(stop.displayName)
                        if (stop.displayAddress != stop.displayName) {
                            rows.append("\n")
                            rows.append(stop.displayAddress)
                        }
                        rows.append("\n\n")
                        services.take(limit).forEach {
                            val isRealTime = RealTimeStatus.IS_REAL_TIME == it.realTimeStatus
                            gotARealTime = gotARealTime or isRealTime
                            rows.append("${it.serviceNumber} (${it.serviceName}) ${TimeUtils.getTimeInDay(it.startTimeInSecs * 1000)}")
                            if (isRealTime) {
                                rows.append("*")
                            }
                            rows.append("\n")
                        }

                        if (gotARealTime) {
                            rows.append("* real-time")
                        }

                        rows.append("\n$shareUrl${region.name}/${stop.code}")

                        rows.toString()
                    }
}