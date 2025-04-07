package com.skedgo.tripkit.ui.timetables

import android.database.Cursor
import com.skedgo.tripkit.data.database.DbFields

/**
 * TODO Should find an appropriate class name
 */
object LoadServiceTaskCursorCols {
    var id: Int = -1
    var waypoints: Int = -1
    var lat: Int = -1
    var lon: Int = -1
    var bearing: Int = -1
    var address: Int = -1
    var name: Int = -1
    var departureTime: Int = -1
    var arrivalTime: Int = -1
    var stopCode: Int = -1
    var realTimeStatus: Int = -1
    var travelled: Int = -1
    var serviceColor: Int = -1
    var wheelchairAccessible: Int = -1

    fun init(cursor: Cursor) {
        id = cursor.getColumnIndex(DbFields.ID.name)
        waypoints = cursor.getColumnIndex(DbFields.WAYPOINT_ENCODING.name)
        travelled = cursor.getColumnIndex(DbFields.TRAVELLED.name)
        lat = cursor.getColumnIndex(DbFields.LAT.name)
        lon = cursor.getColumnIndex(DbFields.LON.name)
        bearing = cursor.getColumnIndex(DbFields.BEARING.name)
        address = cursor.getColumnIndex(DbFields.ADDRESS.name)
        name = cursor.getColumnIndex(DbFields.NAME.name)
        departureTime = cursor.getColumnIndex(DbFields.DEPARTURE_TIME.name)
        arrivalTime = cursor.getColumnIndex(DbFields.ARRIVAL_TIME.name)
        stopCode = cursor.getColumnIndex(DbFields.STOP_CODE.name)
        realTimeStatus = cursor.getColumnIndex(DbFields.REAL_TIME_STATUS.name)
        serviceColor = cursor.getColumnIndex(DbFields.SERVICE_COLOR.name)
        wheelchairAccessible = cursor.getColumnIndex(DbFields.WHEELCHAIR_ACCESSIBLE.name)
    }
}