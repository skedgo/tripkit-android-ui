package com.skedgo.tripkit.ui.favorites.v2.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint
import java.util.UUID

enum class FavoriteType {
    @Deprecated("type from server will be just location, do not pass on POST requests")
    home,

    @Deprecated("type from server will be just location, do not pass on POST requests")
    work,
    location,
    stop,
    trip
}

enum class FavoriteSort(val sortId: Int) {
    home(0),
    work(1)
}

const val FAVORITE_NAME_WORK = "Work"
const val FAVORITE_NAME_HOME = "Home"

// Extension function to convert enum to string
fun FavoriteType.toStringValue(): String {
    return this.name
}

// Extension function to convert string to enum
fun String.toFavoriteType(): FavoriteType {
    return FavoriteType.valueOf(this)
}

/**
 * To handle favorites from server and local storage
 */
@Entity(tableName = "favorites_v2")
@TypeConverters(FavoriteTypeConverter::class, PatternConverter::class)
data class FavoriteV2(
    @PrimaryKey val uuid: String,
    var name: String = "",
    val type: FavoriteType,
    val order: Int? = null,
    val region: String? = null,
    val stopCode: String? = null,
    val filter: String? = null,
    @Embedded(prefix = "location_") val location: LocationFavorite? = null,
    @Embedded(prefix = "start_") val startLocation: LocationFavorite? = null,
    @Embedded(prefix = "end_") val endLocation: LocationFavorite? = null,
    val patterns: List<Waypoint>? = null,
    var userId: String? = null
) {

    data class LocationFavorite(
        val address: String,
        val lat: Double,
        val lng: Double,
        val name: String?
    ) {
        companion object {
            fun parseLocation(location: Location): LocationFavorite = LocationFavorite(
                address = location.address,
                lat = location.lat,
                lng = location.lon,
                name = location.name
            )
        }
    }

    fun getFavoriteLocation(): Location? =
        location?.let {
            Location().apply {
                lat = location.lat
                lon = location.lng
                address = location.address
                name = location.name
            }
        }

    fun getStart(): Location? =
        startLocation?.let {
            Location().apply {
                lat = startLocation.lat
                lon = startLocation.lng
                address = startLocation.address
                name = startLocation.name
            }
        }

    fun getEnd(): Location? =
        endLocation?.let {
            Location().apply {
                lat = endLocation.lat
                lon = endLocation.lng
                address = endLocation.address
                name = endLocation.name
            }
        }

    class Builder(
        private val name: String,
        private val type: FavoriteType
    ) {
        private var uuid: String = UUID.randomUUID().toString()
        private var region: String? = null
        private var stopCode: String? = null
        private var order: Int? = null
        private var filter: String? = null
        private var location: LocationFavorite? = null
        private var start: LocationFavorite? = null
        private var end: LocationFavorite? = null
        private var patterns: List<Waypoint>? = null

        fun region(region: String?) = apply { this.region = region }
        fun uuid(uuid: String) = apply { this.uuid = uuid }
        fun stopCode(stopCode: String?) = apply { this.stopCode = stopCode }
        fun order(order: Int) = apply { this.order = order }
        fun filter(filter: String?) = apply { this.filter = filter }
        fun location(location: Location?) = apply {
            this.location = location?.let {
                LocationFavorite(
                    address = location.address,
                    lat = location.lat,
                    lng = location.lon,
                    name = location.name
                )
            }
        }

        fun start(start: Location?) = apply {
            this.start = start?.let {
                LocationFavorite(
                    address = start.address,
                    lat = start.lat,
                    lng = start.lon,
                    name = start.name
                )
            }
        }

        fun end(end: Location?) = apply {
            this.end = end?.let {
                LocationFavorite(
                    address = end.address,
                    lat = end.lat,
                    lng = end.lon,
                    name = end.name
                )
            }
        }

        fun patterns(patterns: List<Waypoint>?) = apply { this.patterns = patterns }

        fun build(): FavoriteV2 {
            return FavoriteV2(
                uuid = uuid,
                name = name,
                order = order,
                region = region,
                type = type,
                stopCode = stopCode,
                filter = filter,
                location = location,
                startLocation = start,
                endLocation = end,
                patterns = patterns
            )
        }
    }
}

class FavoriteTypeConverter {

    @TypeConverter
    fun fromFavoriteType(type: FavoriteType): String {
        return type.name
    }

    @TypeConverter
    fun toFavoriteType(type: String): FavoriteType {
        return FavoriteType.valueOf(type)
    }
}

class PatternConverter {

    @TypeConverter
    fun fromPatternList(patterns: List<Waypoint>?): String? {
        return if (patterns == null) null else Gson().toJson(patterns)
    }

    @TypeConverter
    fun toPatternList(data: String?): List<Waypoint>? {
        if (data == null) return null
        val listType = object : TypeToken<List<Waypoint>>() {}.type
        return Gson().fromJson(data, listType)
    }
}
