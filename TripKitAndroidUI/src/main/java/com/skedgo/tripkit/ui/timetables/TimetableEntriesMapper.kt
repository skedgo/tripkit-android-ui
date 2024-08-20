package com.skedgo.tripkit.ui.timetables

import android.content.ContentValues
import com.google.gson.Gson
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.realtime.RealtimeAlertRepository
import java.util.Random
import javax.inject.Inject

class TimetableEntriesMapper @Inject constructor(
    private val getModeAccessibility: GetModeAccessibility,
    private val gson: Gson,
    private val realtimeAlertRepository: RealtimeAlertRepository
) {

    fun toContentValues(services: List<TimetableEntry>): Array<ContentValues> {
        val serviceValuesList = Array(services.size) { ContentValues() }

        if (services.isNotEmpty()) {
            val random = Random()

            services.forEachIndexed { i, service ->
                val serviceId = random.nextLong()
                serviceValuesList[i].put(DbFields.ID.name, serviceId)
                serviceValuesList[i].put(DbFields.PAIR_IDENTIFIER.name, service.pairIdentifier)
                serviceValuesList[i].put(DbFields.STOP_CODE.name, service.stopCode)
                serviceValuesList[i].put(DbFields.END_STOP_CODE.name, service.endStopCode)
                serviceValuesList[i].put(DbFields.START_TIME.name, service.startTimeInSecs)
                serviceValuesList[i].put(DbFields.END_TIME.name, service.endTimeInSecs)
                serviceValuesList[i].put(
                    DbFields.JULIAN_DAY.name,
                    TimeUtils.getJulianDay(service.startTimeInSecs * 1000)
                )
                serviceValuesList[i].put(DbFields.FREQUENCY.name, service.frequency)
                serviceValuesList[i].put(DbFields.SERVICE_NUMBER.name, service.serviceNumber)
                serviceValuesList[i].put(DbFields.SERVICE_NAME.name, service.serviceName)
                serviceValuesList[i].put(DbFields.SERVICE_TRIP_ID.name, service.serviceTripId)
                serviceValuesList[i].put(DbFields.SERVICE_OPERATOR.name, service.operator)
                serviceValuesList[i].put(DbFields.SEARCH_STRING.name, service.searchString)
                serviceValuesList[i].put(DbFields.SERVICE_TIME.name, service.serviceTime)
                serviceValuesList[i].put(DbFields.SERVICE_DIRECTION.name, service.serviceDirection)
                serviceValuesList[i].put(
                    DbFields.WHEELCHAIR_ACCESSIBLE.name,
                    getModeAccessibility.wheelchair(service)
                )
                serviceValuesList[i].put(
                    DbFields.BICYCLE_ACCESSIBLE.name,
                    getModeAccessibility.bicycle(service)
                )
                service.startStop?.let {
                    serviceValuesList[i].put(DbFields.START_STOP_SHORT_NAME.name, it.shortName)
                }
                service.realTimeStatus?.let {
                    serviceValuesList[i].put(DbFields.REAL_TIME_STATUS.name, it.toString())
                }

                service.modeInfo?.let {
                    serviceValuesList[i].put(DbFields.MODE_INFO.name, gson.toJson(it))
                }

                service.serviceColor?.let {
                    serviceValuesList[i].put(DbFields.SERVICE_COLOR_RED.name, it.red)
                    serviceValuesList[i].put(DbFields.SERVICE_COLOR_BLUE.name, it.blue)
                    serviceValuesList[i].put(DbFields.SERVICE_COLOR_GREEN.name, it.green)
                }

                service.alertHashCodes?.let {
                    realtimeAlertRepository.addAlertHashCodesForId("$serviceId", it.toList())
                }
                serviceValuesList[i].put(DbFields.START_PLATFORM.name, service.startPlatform)
            }
        }
        return serviceValuesList
    }
}

