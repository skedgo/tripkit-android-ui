package com.skedgo.tripkit.ui.database.scheduled_stops

import com.google.gson.Gson
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.routing.ModeInfo
import javax.inject.Inject

class ScheduledStopMapper @Inject constructor(
    private val gson: Gson
) {
    fun mapToDomain(entity: ScheduledStopWithLocation): ScheduledStop {
        val stop = ScheduledStop()
        stop.code = entity.scheduledStop.code
        stop.stopId = entity.scheduledStop.id
        stop.type = entity.scheduledStop.stopType?.let { StopType.from(it) }
        stop.shortName = entity.scheduledStop.shortName
        stop.services = entity.scheduledStop.services
        // Note: parentId setter is private in ScheduledStop, so we can't set it directly
        
        entity.scheduledStop.modeInfo?.let { modeInfoJson ->
            try {
                stop.modeInfo = gson.fromJson(modeInfoJson, ModeInfo::class.java)
            } catch (e: Exception) {
                // Handle parsing error
            }
        }
        
        entity.location?.let { location ->
            stop.mId = location.id
            stop.name = location.name
            stop.address = location.address
            stop.lat = location.lat
            stop.lon = location.lon
            stop.exact = location.exact == 1
            stop.bearing = location.bearing
            stop.locationType = location.locationType
        }
        
        return stop
    }

    fun mapToDomainList(entities: List<ScheduledStopWithLocation>): List<ScheduledStop> {
        return entities.map { mapToDomain(it) }
    }

    fun mapToScheduledStopEntity(domain: ScheduledStop, cellCode: String): ScheduledStopEntity {
        return ScheduledStopEntity(
            code = domain.code ?: "",
            cellCode = cellCode,
            stopType = domain.type?.toString(),
            shortName = domain.shortName,
            services = domain.services,
            parentId = null, // Note: parentId getter is private in ScheduledStop
            isParent = if (domain.hasChildren()) 1 else 0,
            modeInfo = domain.modeInfo?.let { gson.toJson(it) },
            filter = null
        )
    }

    fun mapToLocationEntity(domain: ScheduledStop): LocationEntity {
        return LocationEntity(
            name = domain.name,
            address = domain.address,
            lat = domain.lat,
            lon = domain.lon,
            exact = if (domain.exact) 1 else 0,
            bearing = domain.bearing,
            favourite = 0,
            favouriteSortOrderPosition = 0,
            hasCar = 0,
            hasMotorbike = 0,
            hasTaxi = 0,
            hasBicycle = 0,
            hasPubTrans = 1,
            scheduledStopCode = domain.code,
            locationType = domain.locationType,
            isDynamic = 0
        )
    }

    fun mapToDownloadHistoryEntity(cellCode: String, downloadTime: Long, hashCode: Long): ScheduledStopDownloadHistoryEntity {
        return ScheduledStopDownloadHistoryEntity(
            cellCode = cellCode,
            downloadTime = downloadTime,
            hashCode2 = hashCode
        )
    }
}
