package com.skedgo.tripkit.ui.database.location_history

import com.skedgo.tripkit.common.model.Location
import java.util.*
import javax.inject.Inject

open class LocationHistoryMapper @Inject constructor() {
    fun toEntity(locations: List<Location>): List<LocationHistoryEntity> {
        return locations
            .map {
                LocationHistoryEntity(
                    name = it.name,
                    address = it.address,
                    lat = it.lat,
                    lon = it.lon,
                    exact = it.isExact,
                    bearing = it.bearing,
                    phone = it.phoneNumber ?: "",
                    url = it.url ?: "",
                    timezone = it.timeZone,
                    popularity = it.popularity,
                    locationClass = it.locationClass ?: "",
                    w3w = it.w3w ?: "",
                    wewInfoURL = it.w3wInfoURL ?: "",
                    createdAt = System.currentTimeMillis()
                )
            }
    }

    fun toLocation(entities: List<LocationHistoryEntity>): List<Location> {
        return entities
            .map {
                val result = Location().also { location ->
                    location.name = it.name
                    location.address = it.address
                    location.lat = it.lat
                    location.lon = it.lon
                    location.isExact = it.exact
                    location.bearing = it.bearing
                    location.phoneNumber = it.phone
                    location.url = it.url
                    location.timeZone = it.timezone
                    location.popularity = it.popularity
                    location.locationClass = it.locationClass ?: ""
                    location.w3w = it.w3w
                    location.w3wInfoURL = it.wewInfoURL
                }
                result
            }
    }
}