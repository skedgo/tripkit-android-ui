package com.skedgo.tripkit.ui.timetables;

import android.database.Cursor;

import com.skedgo.tripkit.data.database.DbFields;


/**
 * TODO Should find an appropriate class name
 */
public class LoadServiceTaskCursorCols {

    public static int id = -1;
    public static int waypoints = -1;
    public static int lat = -1;
    public static int lon = -1;
    public static int bearing = -1;
    public static int address = -1;
    public static int name = -1;
    public static int departureTime = -1;
    public static int arrivalTime = -1;
    public static int stopCode = -1;
    public static int realTimeStatus = -1;
    public static int travelled = -1;
    public static int serviceColor = -1;
    public static int wheelchairAccessible = -1;

    public static void init(Cursor cursor) {
        id = cursor.getColumnIndex(DbFields.ID.getName());
        waypoints = cursor.getColumnIndex(DbFields.WAYPOINT_ENCODING.getName());
        travelled = cursor.getColumnIndex(DbFields.TRAVELLED.getName());
        lat = cursor.getColumnIndex(DbFields.LAT.getName());
        lon = cursor.getColumnIndex(DbFields.LON.getName());
        bearing = cursor.getColumnIndex(DbFields.BEARING.getName());
        address = cursor.getColumnIndex(DbFields.ADDRESS.getName());
        name = cursor.getColumnIndex(DbFields.NAME.getName());
        departureTime = cursor.getColumnIndex(DbFields.DEPARTURE_TIME.getName());
        arrivalTime = cursor.getColumnIndex(DbFields.ARRIVAL_TIME.getName());
        stopCode = cursor.getColumnIndex(DbFields.STOP_CODE.getName());
        realTimeStatus = cursor.getColumnIndex(DbFields.REAL_TIME_STATUS.getName());
        serviceColor = cursor.getColumnIndex(DbFields.SERVICE_COLOR.getName());
        wheelchairAccessible = cursor.getColumnIndex(DbFields.WHEELCHAIR_ACCESSIBLE.getName());
    }
}