package com.skedgo.tripkit.ui.data

import android.database.Cursor
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType.Companion.from
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.database.DbTables
import com.skedgo.tripkit.routing.ModeInfo
import javax.inject.Inject

class CursorToStopConverter @Inject constructor(private val gson: Gson) :
    CursorToEntityConverter<ScheduledStop> {
    override fun apply(cursor: Cursor): ScheduledStop {
        val stop = ScheduledStop()
        stop.code = cursor.getString(cursor.getColumnIndex(DbFields.CODE.name))
        stop.mId = cursor.getLong(cursor.getColumnIndex(DbFields.ID.name))
        stop.stopId = cursor.getLong(cursor.getColumnIndex("stop_id"))
        stop.shortName = cursor.getString(cursor.getColumnIndex(DbFields.SHORT_NAME.name))
        stop.lat = cursor.getDouble(cursor.getColumnIndex(DbFields.LAT.name))
        stop.lon = cursor.getDouble(cursor.getColumnIndex(DbFields.LON.name))
        stop.isFavourite(cursor.getInt(cursor.getColumnIndex(DbFields.FAVOURITE.name)) > 0)
        stop.name = cursor.getString(cursor.getColumnIndex(DbFields.NAME.name))
        stop.address = cursor.getString(cursor.getColumnIndex(DbFields.ADDRESS.name))
        stop.services = cursor.getString(cursor.getColumnIndex(DbFields.SERVICES.name))
        stop.type = from(cursor.getString(cursor.getColumnIndex(DbFields.STOP_TYPE.name)))
        stop.modeInfo = gson.fromJson(
            cursor.getString(cursor.getColumnIndex(DbFields.MODE_INFO.name)),
            ModeInfo::class.java
        )
        stop.currentFilter = cursor.getString(cursor.getColumnIndex(DbFields.FILTER.name))
        stop.favouriteSortOrderIndex =
            cursor.getInt(cursor.getColumnIndex(DbFields.FAVOURITE_SORT_ORDER_POSITION.name))
        return stop
    }

    companion object {
        const val REPLACE_WITH_VAR_ARGS: String = "<REPLACE_WITH_VAR_ARGS>"
        @JvmField
        val SELECTION_ALL: String = DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.CELL_CODE +
            " IN (" + REPLACE_WITH_VAR_ARGS + ")" +
            " AND " + DbFields.PARENT_ID + " IS NULL"
        val PROJECTION: Array<String> = arrayOf(
            DbTables.LOCATIONS.toString() + "." + DbFields.ID,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.ID + " as stop_id",
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.CODE,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.CELL_CODE,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.SHORT_NAME,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.STOP_TYPE,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.SERVICES,
            DbTables.LOCATIONS.toString() + "." + DbFields.LAT,
            DbTables.LOCATIONS.toString() + "." + DbFields.LON,
            DbTables.LOCATIONS.toString() + "." + DbFields.FAVOURITE,
            DbTables.LOCATIONS.toString() + "." + DbFields.NAME,
            DbTables.LOCATIONS.toString() + "." + DbFields.ADDRESS,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.IS_PARENT,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.FILTER,
            DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY.toString() + "." + DbFields.DOWNLOAD_TIME,
            DbTables.LOCATIONS.toString() + "." + DbFields.FAVOURITE_SORT_ORDER_POSITION,
            DbTables.LOCATIONS.toString() + "." + DbFields.HAS_CAR,
            DbTables.LOCATIONS.toString() + "." + DbFields.HAS_MOTORBIKE,
            DbTables.LOCATIONS.toString() + "." + DbFields.HAS_TAXI,
            DbTables.LOCATIONS.toString() + "." + DbFields.HAS_BICYCLE,
            DbTables.LOCATIONS.toString() + "." + DbFields.HAS_PUB_TRANS,
            DbTables.SCHEDULED_STOPS.toString() + "." + DbFields.MODE_INFO
        )
    }
}