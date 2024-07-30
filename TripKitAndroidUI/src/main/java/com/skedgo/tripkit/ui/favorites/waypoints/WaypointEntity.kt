package com.skedgo.tripkit.ui.favorites.waypoints

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(
    tableName = "waypoints"
)
@TypeConverters(WaypointStringListConverter::class)
class WaypointEntity(
    var lat: Double,
    var lng: Double,
    var mode: String?,
    var modes: List<String>?,
    var modeTitle: String?,
    var order: Int,
    var tripId: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}

class WaypointStringListConverter {

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return if (list == null) null else Gson().toJson(list)
    }

    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        if (data == null) return null
        val listType = object : TypeToken<List<Waypoint>>() {}.type
        return Gson().fromJson(data, listType)
    }
}