package com.skedgo.tripkit.ui.timetables;

import android.content.Context;

import com.skedgo.tripkit.common.util.TimeUtils;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.data.database.DbTables;
import com.skedgo.tripkit.ui.provider.ServiceStopsProvider;

import androidx.loader.content.CursorLoader;

public class ServiceStopsLoaderFactory {
    public static final String[] PROJECTION = new String[]{
        DbTables.SEGMENT_SHAPES + "." + DbFields.ID,
        DbTables.SEGMENT_SHAPES + "." + DbFields.TRAVELLED,
        DbTables.SEGMENT_SHAPES + "." + DbFields.WAYPOINT_ENCODING,
        DbTables.SERVICE_STOPS + "." + DbFields.STOP_CODE,
        DbTables.SERVICE_STOPS + "." + DbFields.DEPARTURE_TIME,
        DbTables.SERVICE_STOPS + "." + DbFields.ARRIVAL_TIME,
        DbTables.SERVICE_STOPS + "." + DbFields.STOP_TYPE,
        DbTables.SERVICE_STOPS + "." + DbFields.WHEELCHAIR_ACCESSIBLE,
        DbTables.LOCATIONS + "." + DbFields.LAT,
        DbTables.LOCATIONS + "." + DbFields.LON,
        DbTables.LOCATIONS + "." + DbFields.BEARING,
        DbTables.LOCATIONS + "." + DbFields.ADDRESS,
        DbTables.LOCATIONS + "." + DbFields.NAME,
        DbTables.SEGMENT_SHAPES + "." + DbFields.SERVICE_COLOR,
        DbTables.SCHEDULED_SERVICES + "." + DbFields.REAL_TIME_STATUS
    };

    /**
     * segment_shapes.service_trip_id = ? AND (service_stops.julian_day = ? OR service_stops.julian_day = 0)
     */
    public static final String SELECTION = DbTables.SEGMENT_SHAPES + "." + DbFields.SERVICE_TRIP_ID
        + " =? AND (" + DbTables.SERVICE_STOPS + "." + DbFields.JULIAN_DAY + " =? OR "
        + DbTables.SERVICE_STOPS + "." + DbFields.JULIAN_DAY + " = 0)";

    /**
     * service_stops.arrival_time ASC, service_stops.departure_time ASC
     */
    public static final String SORT_ORDER = DbTables.SERVICE_STOPS + "." + DbFields.ARRIVAL_TIME + " ASC, "
        + DbTables.SERVICE_STOPS + "." + DbFields.DEPARTURE_TIME + " ASC";

    public static CursorLoader create(Context context, String serviceTripId, long timeInSeconds) {
        return new CursorLoader(context,
            ServiceStopsProvider.Companion.getSTOPS_BY_SERVICE_URI(), PROJECTION, SELECTION,

            /* selectionArgs */
            new String[]{
                serviceTripId,
                /* TODO: Fix time zone! */
                String.valueOf(TimeUtils.getJulianDay(timeInSeconds * TimeUtils.InMillis.SECOND))
            },

            SORT_ORDER
        );
    }
}