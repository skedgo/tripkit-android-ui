package com.skedgo.tripkit.ui.data;

import android.database.Cursor;

import com.google.gson.Gson;
import com.skedgo.tripkit.common.model.stop.ScheduledStop;
import com.skedgo.tripkit.common.model.stop.StopType;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.data.database.DbTables;
import com.skedgo.tripkit.routing.ModeInfo;

import javax.inject.Inject;

public class CursorToStopConverter implements CursorToEntityConverter<ScheduledStop> {
    public static final String REPLACE_WITH_VAR_ARGS = "<REPLACE_WITH_VAR_ARGS>";
    public static final String SELECTION_ALL = DbTables.SCHEDULED_STOPS + "." + DbFields.CELL_CODE +
        " IN (" + REPLACE_WITH_VAR_ARGS + ")" +
        " AND " + DbFields.PARENT_ID + " IS NULL";
    public static final String[] PROJECTION = new String[]{
        DbTables.LOCATIONS + "." + DbFields.ID,
        DbTables.SCHEDULED_STOPS + "." + DbFields.ID + " as stop_id",
        DbTables.SCHEDULED_STOPS + "." + DbFields.CODE,
        DbTables.SCHEDULED_STOPS + "." + DbFields.CELL_CODE,
        DbTables.SCHEDULED_STOPS + "." + DbFields.SHORT_NAME,
        DbTables.SCHEDULED_STOPS + "." + DbFields.STOP_TYPE,
        DbTables.SCHEDULED_STOPS + "." + DbFields.SERVICES,
        DbTables.LOCATIONS + "." + DbFields.LAT,
        DbTables.LOCATIONS + "." + DbFields.LON,
        DbTables.LOCATIONS + "." + DbFields.FAVOURITE,
        DbTables.LOCATIONS + "." + DbFields.NAME,
        DbTables.LOCATIONS + "." + DbFields.ADDRESS,
        DbTables.SCHEDULED_STOPS + "." + DbFields.IS_PARENT,
        DbTables.SCHEDULED_STOPS + "." + DbFields.FILTER,
        DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY + "." + DbFields.DOWNLOAD_TIME,
        DbTables.LOCATIONS + "." + DbFields.FAVOURITE_SORT_ORDER_POSITION,
        DbTables.LOCATIONS + "." + DbFields.HAS_CAR,
        DbTables.LOCATIONS + "." + DbFields.HAS_MOTORBIKE,
        DbTables.LOCATIONS + "." + DbFields.HAS_TAXI,
        DbTables.LOCATIONS + "." + DbFields.HAS_BICYCLE,
        DbTables.LOCATIONS + "." + DbFields.HAS_PUB_TRANS,
        DbTables.SCHEDULED_STOPS + "." + DbFields.MODE_INFO
    };
    private final Gson gson;

    @Inject
    public CursorToStopConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public ScheduledStop apply(Cursor cursor) {
        final ScheduledStop stop = new ScheduledStop();
        stop.setCode(cursor.getString(cursor.getColumnIndex(DbFields.CODE.name)));
        stop.setMId(cursor.getLong(cursor.getColumnIndex(DbFields.ID.name)));
        stop.setStopId(cursor.getLong(cursor.getColumnIndex("stop_id")));
        stop.setShortName(cursor.getString(cursor.getColumnIndex(DbFields.SHORT_NAME.name)));
        stop.setLat(cursor.getDouble(cursor.getColumnIndex(DbFields.LAT.name)));
        stop.setLon(cursor.getDouble(cursor.getColumnIndex(DbFields.LON.name)));
        stop.isFavourite(cursor.getInt(cursor.getColumnIndex(DbFields.FAVOURITE.name)) > 0);
        stop.setName(cursor.getString(cursor.getColumnIndex(DbFields.NAME.name)));
        stop.setAddress(cursor.getString(cursor.getColumnIndex(DbFields.ADDRESS.name)));
        stop.setServices(cursor.getString(cursor.getColumnIndex(DbFields.SERVICES.name)));
        stop.setType(StopType.from(cursor.getString(cursor.getColumnIndex(DbFields.STOP_TYPE.name))));
        stop.setModeInfo(gson.fromJson(cursor.getString(cursor.getColumnIndex(DbFields.MODE_INFO.name)), ModeInfo.class));
        stop.setCurrentFilter(cursor.getString(cursor.getColumnIndex(DbFields.FILTER.name)));
        stop.setFavouriteSortOrderIndex(cursor.getInt(cursor.getColumnIndex(DbFields.FAVOURITE_SORT_ORDER_POSITION.name)));
        return stop;
    }
}