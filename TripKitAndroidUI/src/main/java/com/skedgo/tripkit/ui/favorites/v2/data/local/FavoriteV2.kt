package com.skedgo.tripkit.ui.favorites.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteV2.Pattern
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
@TypeConverters(FavoriteTypeConverter::class, PatternConverter::class, LocationConverter::class)
data class FavoriteV2(
    @PrimaryKey val uuid: String,
    val name: String,
    val type: FavoriteType,
    val order: Int? = null,
    val region: String? = null,
    val stopCode: String? = null,
    val filter: String? = null,
    val location: Location? = null,
    val start: Location? = null,
    val end: Location? = null,
    val patterns: List<Pattern>? = null,
    var userId: String? = null
) {

    data class Pattern(
        val end: String,
        val modes: List<String>,
        val start: String
    )

    class Builder(
        private val name: String,
        private val type: FavoriteType
    ) {
        private var uuid: String = UUID.randomUUID().toString()
        private var region: String? = null
        private var stopCode: String? = null
        private var order: Int? = null
        private var filter: String? = null
        private var location: Location? = null
        private var start: Location? = null
        private var end: Location? = null
        private var patterns: List<Pattern>? = null

        fun region(region: String?) = apply { this.region = region }
        fun uuid(uuid: String) = apply { this.uuid = uuid }
        fun stopCode(stopCode: String?) = apply { this.stopCode = stopCode }
        fun order(order: Int) = apply { this.order = order }
        fun filter(filter: String?) = apply { this.filter = filter }
        fun location(location: Location?) = apply { this.location = location }
        fun start(start: Location?) = apply { this.start = start }
        fun end(end: Location?) = apply { this.end = end }
        fun patterns(patterns: List<Pattern>?) = apply { this.patterns = patterns }

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
                start = start,
                end = end,
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
    fun fromPatternList(patterns: List<Pattern>?): String? {
        return if (patterns == null) null else Gson().toJson(patterns)
    }

    @TypeConverter
    fun toPatternList(data: String?): List<Pattern>? {
        if (data == null) return null
        val listType = object : TypeToken<List<Pattern>>() {}.type
        return Gson().fromJson(data, listType)
    }
}


class LocationConverter {

    @TypeConverter
    fun fromLocation(location: Location?): String? {
        return if (location == null) {
            null
        } else {
            Gson().toJson(location)
        }
    }

    @TypeConverter
    fun toLocation(locationString: String?): Location? {
        return if (locationString == null) {
            null
        } else {
            Gson().fromJson(locationString, Location::class.java)
        }
    }
}
