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
        id = cursor.getColumnIndex(DbFields.ID.name);
        waypoints = cursor.getColumnIndex(DbFields.WAYPOINT_ENCODING.name);
        travelled = cursor.getColumnIndex(DbFields.TRAVELLED.name);
        lat = cursor.getColumnIndex(DbFields.LAT.name);
        lon = cursor.getColumnIndex(DbFields.LON.name);
        bearing = cursor.getColumnIndex(DbFields.BEARING.name);
        address = cursor.getColumnIndex(DbFields.ADDRESS.name);
        name = cursor.getColumnIndex(DbFields.NAME.name);
        departureTime = cursor.getColumnIndex(DbFields.DEPARTURE_TIME.name);
        arrivalTime = cursor.getColumnIndex(DbFields.ARRIVAL_TIME.name);
        stopCode = cursor.getColumnIndex(DbFields.STOP_CODE.name);
        realTimeStatus = cursor.getColumnIndex(DbFields.REAL_TIME_STATUS.name);
        serviceColor = cursor.getColumnIndex(DbFields.SERVICE_COLOR.name);
        wheelchairAccessible = cursor.getColumnIndex(DbFields.WHEELCHAIR_ACCESSIBLE.name);
    }
}