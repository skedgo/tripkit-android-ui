package com.skedgo.tripkit.ui.database.location_history

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skedgo.routepersistence.LocationTypeAdapterFactory
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import javax.inject.Inject

open class LocationHistoryMapper @Inject constructor() {

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapterFactory(LocationTypeAdapterFactory())
            .create()
    }

    fun toEntity(locations: List<Location>): List<LocationHistoryEntity> {
        return locations.map { location ->
            val locationType = when {
                location is ScheduledStop -> Location.TYPE_SCHEDULED_STOP
                location.locationType != Location.TYPE_UNKNOWN -> location.locationType
                else -> Location.TYPE_HISTORY
            }

            LocationHistoryEntity(
                name = location.name,
                address = location.displayAddress,
                lat = location.lat,
                lon = location.lon,
                exact = location.exact,
                bearing = location.bearing,
                phone = location.phoneNumber ?: "",
                url = location.url ?: "",
                timezone = location.timeZone,
                popularity = location.popularity,
                locationClass = location.locationClass ?: "",
                w3w = location.w3w ?: "",
                wewInfoURL = location.w3wInfoURL ?: "",
                createdAt = System.currentTimeMillis(),
                locationType = locationType,
                locationJson = runCatching { gson.toJson(location) }.getOrNull()
            )
        }
    }

    fun toLocation(entities: List<LocationHistoryEntity>): List<Location> {
        return entities.map { entity ->
            val parsedFromJson = entity.locationJson
                ?.takeIf { it.isNotBlank() }
                ?.let { json ->
                    val parsed = when (entity.locationType) {
                        Location.TYPE_SCHEDULED_STOP ->
                            runCatching { gson.fromJson(json, ScheduledStop::class.java) }.getOrNull()
                                ?: runCatching { gson.fromJson(json, Location::class.java) }.getOrNull()

                        else -> runCatching { gson.fromJson(json, Location::class.java) }.getOrNull()
                    }
                    parsed?.apply {
                        if (locationType == Location.TYPE_UNKNOWN && entity.locationType != Location.TYPE_UNKNOWN) {
                            locationType = entity.locationType
                        }
                    }
                }

            parsedFromJson ?: run {
                val location = when (entity.locationType) {
                    Location.TYPE_SCHEDULED_STOP -> ScheduledStop()
                    else -> Location()
                }
                location.name = entity.name
                location.address = entity.address
                location.lat = entity.lat
                location.lon = entity.lon
                location.exact = entity.exact
                location.bearing = entity.bearing
                location.phoneNumber = entity.phone
                location.url = entity.url
                location.timeZone = entity.timezone
                location.popularity = entity.popularity
                location.locationClass = entity.locationClass ?: ""
                location.w3w = entity.w3w
                location.w3wInfoURL = entity.wewInfoURL
                location.locationType = entity.locationType
                location
            }
        }
    }
}
