package com.skedgo.tripkit.ui.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.skedgo.tripkit.common.model.RealTimeStatus;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.ui.model.TimetableEntry;
import org.jetbrains.annotations.NotNull;
import com.skedgo.tripkit.routing.ModeInfo;
import com.skedgo.tripkit.routing.ServiceColor;
import com.skedgo.tripkit.routing.VehicleMode;

import javax.inject.Inject;

import static com.skedgo.tripkit.data.database.DbFields.*;

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
    service.setStartStopShortName(getStartStopShortName());
    final String modeInfoJson = cursor.getString(cursor.getColumnIndex(DbFields.MODE_INFO.getName()));
    if (modeInfoJson != null) {
      final ModeInfo modeInfo = gson.fromJson(modeInfoJson, ModeInfo.class);
      service.setModeInfo(modeInfo);
    }

    // TODO: What about hasAlerts()?
    service.setServiceDirection(cursor.getString(cursor.getColumnIndex(DbFields.SERVICE_DIRECTION.getName())));
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
    public int startStopShortName = -1;

    public void getColumnIndices(@NotNull Cursor cursor) {
      // The '_id' column always appears on query result.
      // So we utilize it to check if all the indices are retrieved.
      if (idIndex != -1) {
        // Okay, previously cached.
        // Let's ignore the rest!
        return;
      }

      idIndex = cursor.getColumnIndex(ID.getName());
      pairIdentifierIndex = cursor.getColumnIndex(PAIR_IDENTIFIER.getName());
      stopCodeIndex = cursor.getColumnIndex(STOP_CODE.getName());
      endStopCodeIndex = cursor.getColumnIndex(END_STOP_CODE.getName());
      modeIndex = cursor.getColumnIndex(MODE.getName());
      startTimeIndex = cursor.getColumnIndex(START_TIME.getName());
      endTimeIndex = cursor.getColumnIndex(END_TIME.getName());
      julianDayIndex = cursor.getColumnIndex(JULIAN_DAY.getName());
      frequencyIndex = cursor.getColumnIndex(FREQUENCY.getName());
      serviceNumberIndex = cursor.getColumnIndex(SERVICE_NUMBER.getName());
      serviceNameIndex = cursor.getColumnIndex(SERVICE_NAME.getName());
      serviceTripIdIndex = cursor.getColumnIndex(SERVICE_TRIP_ID.getName());
      serviceColorRedIndex = cursor.getColumnIndex(SERVICE_COLOR_RED.getName());
      serviceColorBlueIndex = cursor.getColumnIndex(SERVICE_COLOR_BLUE.getName());
      serviceColorGreenIndex = cursor.getColumnIndex(SERVICE_COLOR_GREEN.getName());
      realTimeStatusIndex = cursor.getColumnIndex(REAL_TIME_STATUS.getName());
      favouriteIndex = cursor.getColumnIndex(FAVOURITE.getName());
      hasAlertsIndex = cursor.getColumnIndex(HAS_ALERTS.getName());
      searchStringIndex = cursor.getColumnIndex(SEARCH_STRING.getName());
      serviceTimeIndex = cursor.getColumnIndex(SERVICE_TIME.getName());
      serviceOperator = cursor.getColumnIndex(SERVICE_OPERATOR.getName());
      wheelchairAccessible = cursor.getColumnIndex(WHEELCHAIR_ACCESSIBLE.getName());
      startStopShortName = cursor.getColumnIndex(START_STOP_SHORT_NAME.getName());
    }
  }
}