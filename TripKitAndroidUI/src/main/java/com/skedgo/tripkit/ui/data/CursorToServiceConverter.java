package com.skedgo.tripkit.ui.data;

import android.database.Cursor;

import com.google.gson.Gson;
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.routing.ModeInfo;
import com.skedgo.tripkit.routing.ServiceColor;
import com.skedgo.tripkit.routing.VehicleMode;
import com.skedgo.tripkit.ui.model.TimetableEntry;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import androidx.annotation.NonNull;

import static com.skedgo.tripkit.data.database.DbFields.BICYCLE_ACCESSIBLE;
import static com.skedgo.tripkit.data.database.DbFields.END_STOP_CODE;
import static com.skedgo.tripkit.data.database.DbFields.END_TIME;
import static com.skedgo.tripkit.data.database.DbFields.FAVOURITE;
import static com.skedgo.tripkit.data.database.DbFields.FREQUENCY;
import static com.skedgo.tripkit.data.database.DbFields.HAS_ALERTS;
import static com.skedgo.tripkit.data.database.DbFields.ID;
import static com.skedgo.tripkit.data.database.DbFields.JULIAN_DAY;
import static com.skedgo.tripkit.data.database.DbFields.MODE;
import static com.skedgo.tripkit.data.database.DbFields.PAIR_IDENTIFIER;
import static com.skedgo.tripkit.data.database.DbFields.REAL_TIME_STATUS;
import static com.skedgo.tripkit.data.database.DbFields.SEARCH_STRING;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_COLOR_BLUE;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_COLOR_GREEN;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_COLOR_RED;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_NAME;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_NUMBER;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_OPERATOR;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_TIME;
import static com.skedgo.tripkit.data.database.DbFields.SERVICE_TRIP_ID;
import static com.skedgo.tripkit.data.database.DbFields.START_PLATFORM;
import static com.skedgo.tripkit.data.database.DbFields.START_STOP_SHORT_NAME;
import static com.skedgo.tripkit.data.database.DbFields.START_TIME;
import static com.skedgo.tripkit.data.database.DbFields.STOP_CODE;
import static com.skedgo.tripkit.data.database.DbFields.WHEELCHAIR_ACCESSIBLE;

public class CursorToServiceConverter implements CursorToEntityConverter<TimetableEntry> {
    private final Gson gson;
    private ServiceColumnIndices mServiceColumnIndices = new ServiceColumnIndices();
    private Cursor mCursor;

    @Inject
    public CursorToServiceConverter(@NonNull Gson gson) {
        this.gson = gson;
    }

    @Override
    public TimetableEntry apply(@NotNull Cursor cursor) {
        setCursor(cursor);

        TimetableEntry service = new TimetableEntry();
        service.setId(getId());
        service.pairIdentifier = getPairIdentifier();
        service.setStopCode(getStopCode());
        service.setEndStopCode(getEndStopCode());
        service.setMode(getMode());
        service.setStartTimeInSecs(getStartTimeInSecs());
        service.setEndTimeInSecs(getEndTimeInSecs());
        service.setFrequency(getFrequency());
        service.setServiceNumber(getServiceNumber());
        service.setServiceName(getServiceName());
        service.setServiceTripId(getServiceTripId());
        service.setServiceColor(new ServiceColor(
            getServiceColorRed(),
            getServiceColorGreen(),
            getServiceColorBlue()
        ));
        service.setOperator(getServiceOperator());
        service.setRealTimeStatus(getRealTimeStatus());
        if (cursor.getColumnIndex("realTimeDeparture") != -1) {
            service.setRealTimeDeparture(cursor.getInt(cursor.getColumnIndex("realTimeDeparture")));
        }

        if (cursor.getColumnIndex("realTimeArrival") != -1) {
            service.setRealTimeArrival(cursor.getInt(cursor.getColumnIndex("realTimeArrival")));
        }
        service.isFavourite(isFavourite());
        service.setSearchString(getSearchString());
        service.setServiceTime(getServiceTime());
        service.setWheelchairAccessible(getWheelchairAccessible());
        service.setBicycleAccessible(getBicycleAccessible());
        service.setStartStopShortName(getStartStopShortName());
        final String modeInfoJson = cursor.getString(cursor.getColumnIndex(DbFields.MODE_INFO.name));
        if (modeInfoJson != null) {
            final ModeInfo modeInfo = gson.fromJson(modeInfoJson, ModeInfo.class);
            service.setModeInfo(modeInfo);
        }

        // TODO: What about hasAlerts()?
        service.setServiceDirection(cursor.getString(cursor.getColumnIndex(DbFields.SERVICE_DIRECTION.name)));
        service.setStartPlatform(cursor.getString(cursor.getColumnIndex(DbFields.START_PLATFORM.name)));
        return service;
    }

    public void setCursor(@NotNull Cursor cursor) {
        mCursor = cursor;

        // Cache column indices if necessary
        mServiceColumnIndices.getColumnIndices(cursor);
    }

    public long getId() {
        return mCursor.getInt(mServiceColumnIndices.idIndex);
    }

    public String getPairIdentifier() {
        return mCursor.getString(mServiceColumnIndices.pairIdentifierIndex);
    }

    public String getStopCode() {
        return mCursor.getString(mServiceColumnIndices.stopCodeIndex);
    }

    public String getEndStopCode() {
        return mCursor.getString(mServiceColumnIndices.endStopCodeIndex);
    }

    public VehicleMode getMode() {
        return VehicleMode.from(mCursor.getString(mServiceColumnIndices.modeIndex));
    }

    public long getStartTimeInSecs() {
        return mCursor.getLong(mServiceColumnIndices.startTimeIndex);
    }

    public long getEndTimeInSecs() {
        return mCursor.getLong(mServiceColumnIndices.endTimeIndex);
    }

    public int getFrequency() {
        return mCursor.getInt(mServiceColumnIndices.frequencyIndex);
    }

    public String getServiceNumber() {
        return mCursor.getString(mServiceColumnIndices.serviceNumberIndex);
    }

    public String getServiceName() {
        return mCursor.getString(mServiceColumnIndices.serviceNameIndex);
    }

    public String getServiceTripId() {
        return mCursor.getString(mServiceColumnIndices.serviceTripIdIndex);
    }

    public int getServiceColorRed() {
        return mCursor.getInt(mServiceColumnIndices.serviceColorRedIndex);
    }

    public int getServiceColorGreen() {
        return mCursor.getInt(mServiceColumnIndices.serviceColorGreenIndex);
    }

    public int getServiceColorBlue() {
        return mCursor.getInt(mServiceColumnIndices.serviceColorBlueIndex);
    }

    public RealTimeStatus getRealTimeStatus() {
        return RealTimeStatus.from(mCursor.getString(mServiceColumnIndices.realTimeStatusIndex));
    }

    public boolean isFavourite() {
        return mCursor.getInt(mServiceColumnIndices.favouriteIndex) > 0;
    }

    public String getSearchString() {
        return mCursor.getString(mServiceColumnIndices.searchStringIndex);
    }

    public String getServiceOperator() {
        return mCursor.getString(mServiceColumnIndices.serviceOperator);
    }

    public long getServiceTime() {
        return mCursor.getLong(mServiceColumnIndices.serviceTimeIndex);
    }

    public Boolean getWheelchairAccessible() {
        int wheelchairAccessible = mCursor.getInt(mServiceColumnIndices.wheelchairAccessible);
        switch (wheelchairAccessible) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                return null;
        }
    }

    public Boolean getBicycleAccessible() {
        int bicycleAccessible = mCursor.getInt(mServiceColumnIndices.bicycleAccessible);
        switch (bicycleAccessible) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                return null;
        }
    }

    public String getStartStopShortName() {
        return mCursor.getString(mServiceColumnIndices.startStopShortName);
    }

    /**
     * NOTE: Don't hard code column indices
     */
    public static class ServiceColumnIndices {
        public int idIndex = -1;
        public int pairIdentifierIndex = -1;
        public int stopCodeIndex = -1;
        public int endStopCodeIndex = -1;
        public int modeIndex = -1;
        public int startTimeIndex = -1;
        public int endTimeIndex = -1;
        public int julianDayIndex = -1;
        public int frequencyIndex = -1;
        public int serviceNumberIndex = -1;
        public int serviceNameIndex = -1;
        public int serviceTripIdIndex = -1;
        public int serviceColorRedIndex = -1;
        public int serviceColorBlueIndex = -1;
        public int serviceColorGreenIndex = -1;
        public int realTimeStatusIndex = -1;
        public int favouriteIndex = -1;
        public int hasAlertsIndex = -1;
        public int searchStringIndex = -1;
        public int serviceTimeIndex = -1;
        public int serviceOperator = -1;
        public int wheelchairAccessible = -1;
        public int bicycleAccessible = -1;
        public int startStopShortName = -1;
        public int startPlatform = -1;

        public void getColumnIndices(@NotNull Cursor cursor) {
            // The '_id' column always appears on query result.
            // So we utilize it to check if all the indices are retrieved.
            if (idIndex != -1) {
                // Okay, previously cached.
                // Let's ignore the rest!
                return;
            }

            idIndex = cursor.getColumnIndex(ID.name);
            pairIdentifierIndex = cursor.getColumnIndex(PAIR_IDENTIFIER.name);
            stopCodeIndex = cursor.getColumnIndex(STOP_CODE.name);
            endStopCodeIndex = cursor.getColumnIndex(END_STOP_CODE.name);
            modeIndex = cursor.getColumnIndex(MODE.name);
            startTimeIndex = cursor.getColumnIndex(START_TIME.name);
            endTimeIndex = cursor.getColumnIndex(END_TIME.name);
            julianDayIndex = cursor.getColumnIndex(JULIAN_DAY.name);
            frequencyIndex = cursor.getColumnIndex(FREQUENCY.name);
            serviceNumberIndex = cursor.getColumnIndex(SERVICE_NUMBER.name);
            serviceNameIndex = cursor.getColumnIndex(SERVICE_NAME.name);
            serviceTripIdIndex = cursor.getColumnIndex(SERVICE_TRIP_ID.name);
            serviceColorRedIndex = cursor.getColumnIndex(SERVICE_COLOR_RED.name);
            serviceColorBlueIndex = cursor.getColumnIndex(SERVICE_COLOR_BLUE.name);
            serviceColorGreenIndex = cursor.getColumnIndex(SERVICE_COLOR_GREEN.name);
            realTimeStatusIndex = cursor.getColumnIndex(REAL_TIME_STATUS.name);
            favouriteIndex = cursor.getColumnIndex(FAVOURITE.name);
            hasAlertsIndex = cursor.getColumnIndex(HAS_ALERTS.name);
            searchStringIndex = cursor.getColumnIndex(SEARCH_STRING.name);
            serviceTimeIndex = cursor.getColumnIndex(SERVICE_TIME.name);
            serviceOperator = cursor.getColumnIndex(SERVICE_OPERATOR.name);
            wheelchairAccessible = cursor.getColumnIndex(WHEELCHAIR_ACCESSIBLE.name);
            bicycleAccessible = cursor.getColumnIndex(BICYCLE_ACCESSIBLE.name);
            startStopShortName = cursor.getColumnIndex(START_STOP_SHORT_NAME.name);
            startPlatform = cursor.getColumnIndex(START_PLATFORM.name);
        }
    }
}