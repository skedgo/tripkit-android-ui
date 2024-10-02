package com.skedgo.tripkit.ui.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.skedgo.tripkit.common.agenda.IRealTimeElement;
import com.skedgo.tripkit.common.model.BicycleAccessible;
import com.skedgo.tripkit.common.model.time.ITimeRange;
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus;
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert;
import com.skedgo.tripkit.common.model.stop.ScheduledStop;
import com.skedgo.tripkit.common.model.WheelchairAccessible;
import com.skedgo.tripkit.common.rx.Var;
import com.skedgo.tripkit.routing.ModeInfo;
import com.skedgo.tripkit.routing.RealTimeVehicle;
import com.skedgo.tripkit.routing.ServiceColor;
import com.skedgo.tripkit.routing.VehicleMode;
import com.skedgo.tripkit.ui.BuildConfig;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import io.reactivex.functions.Consumer;

/**
 * (Aka Service)
 */
public class TimetableEntry implements Parcelable, IRealTimeElement, ITimeRange, WheelchairAccessible, BicycleAccessible {
    public static final Creator<TimetableEntry> CREATOR = new Creator<TimetableEntry>() {
        public TimetableEntry createFromParcel(Parcel in) {
            TimetableEntry service = new TimetableEntry();

            service.id = in.readLong();
            service.stopCode = in.readString();
            service.serviceTripId = in.readString();
            service.serviceNumber = in.readString();
            service.serviceName = in.readString();
            service.realTimeStatus = RealTimeStatus.from(in.readString());
            service.serializedStartSecs = in.readLong();
            service.serializedEndSecs = in.readLong();
            service.serviceColor = in.readParcelable(ServiceColor.class.getClassLoader());
            service.frequency = in.readInt();
            service.isFavourite = in.readInt() == 1;
            service.alerts = in.readArrayList(RealtimeAlert.class.getClassLoader());
            service.searchString = in.readString();
            service.endStopCode = in.readString();
            service.startStop = in.readParcelable(ScheduledStop.class.getClassLoader());
            service.endStop = in.readParcelable(ScheduledStop.class.getClassLoader());
            service.mode = VehicleMode.from(in.readString());
            service.pairIdentifier = in.readString();
            service.operator = in.readString();
            service.realtimeVehicle = in.readParcelable(RealTimeVehicle.class.getClassLoader());
            service.serviceTime = in.readLong();
            service.modeInfo = in.readParcelable(ModeInfo.class.getClassLoader());
            service.serviceDirection = in.readString();
            service.wheelchairAccessible = (Boolean) in.readValue(Boolean.class.getClassLoader());
            service.bicycleAccessible = (Boolean) in.readValue(Boolean.class.getClassLoader());
            service.startStopShortName = in.readString();
            service.alertHashCodes = in.readArrayList(Long.class.getClassLoader());
            service.serviceColor = in.readParcelable(ServiceColor.class.getClassLoader());
            service.startPlatform = in.readString();
            return service;
        }

        public TimetableEntry[] newArray(int size) {
            return new TimetableEntry[size];
        }
    };
    public final transient Var<List<StopInfo>> stops = Var.create();
    /**
     * For A2B-timetable-related stuff.
     */
    public transient ScheduledStop startStop;
    /**
     * For A2B-timetable-related stuff.
     */
    public transient ScheduledStop endStop;
    /**
     * For A2B-timetable-related stuff.
     */
    public String pairIdentifier;
    private transient long id;
    private transient boolean isFavourite;
    @SerializedName("realtimeVehicle")
    private RealTimeVehicle realtimeVehicle;
    @SerializedName("stopCode")
    private String stopCode;
    @SerializedName("modeInfo")
    private ModeInfo modeInfo;
    @SerializedName("operator")
    private String operator;
    @SerializedName("endStopCode")
    private String endStopCode;
    @SerializedName("serviceTripID")
    private String serviceTripId;
    @SerializedName("serviceNumber")
    private String serviceNumber;
    @SerializedName("serviceName")
    private String serviceName;
    @SerializedName("serviceDirection")
    private String serviceDirection;
    @SerializedName("realTimeStatus")
    private RealTimeStatus realTimeStatus;
    @SerializedName("realTimeDeparture")
    private int realTimeDeparture = -1;
    @SerializedName("realTimeArrival")
    private int realTimeArrival = -1;

    @SerializedName("alerts")
    private ArrayList<RealtimeAlert> alerts;
    @SerializedName("serviceColor")
    private ServiceColor serviceColor;
    @SerializedName("frequency")
    private int frequency;
    @SerializedName("searchString")
    private String searchString;
    @SerializedName("alertHashCodes")
    private @Nullable ArrayList<Long> alertHashCodes;
    @SerializedName("wheelchairAccessible")
    private @Nullable Boolean wheelchairAccessible;
    @SerializedName("bicycleAccessible")
    private @Nullable Boolean bicycleAccessible;
    @SerializedName("start_stop_short_name")
    private String startStopShortName;
    @SerializedName("startPlatform")
    private String startPlatform;
    /**
     * Replacement: {@link #modeInfo}.
     */
    @Deprecated
    @SerializedName("mode")
    private VehicleMode mode;
    /**
     * This field is primarily used to interact with Gson or Parcel.
     */
    @SerializedName("startTime")
    private long serializedStartSecs;
    /**
     * This field is primarily used to interact with Gson or Parcel.
     */
    @SerializedName("endTime")
    private long serializedEndSecs;
    /**
     * Service time is initially the same as "startTime". If is a realtime service, here we save the
     * service time, while startTime will have the real arriving time.
     */
    private long serviceTime;

    public TimetableEntry() {
        // For debug purpose only.
        if (BuildConfig.DEBUG) {
            stops.observe().subscribe(new Consumer<List<StopInfo>>() {
                @Override
                public void accept(List<StopInfo> stops) {
                    Log.w("LoadStops", "Got " + stops.size() + " stops for: " + serviceNumber + " - " + TimetableEntry.this);
                }
            });
        }
    }

    @Nullable
    public String getServiceDirection() {
        return serviceDirection;
    }

    public void setServiceDirection(String serviceDirection) {
        this.serviceDirection = serviceDirection;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStopCode() {
        return stopCode;
    }

    public void setStopCode(String stopCode) {
        this.stopCode = stopCode;
    }

    public String getServiceTripId() {
        return serviceTripId;
    }

    public void setServiceTripId(String serviceTripId) {
        this.serviceTripId = serviceTripId;
    }

    @Nullable
    public String getServiceNumber() {
        return serviceNumber;
    }

    public void setServiceNumber(String serviceNumber) {
        this.serviceNumber = serviceNumber;
    }

    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public RealTimeStatus getRealTimeStatus() {
        return realTimeStatus;
    }

    public void setRealTimeStatus(RealTimeStatus realTimeStatus) {
        this.realTimeStatus = realTimeStatus;
    }

    @Override
    public long getStartTimeInSecs() {
        return serializedStartSecs;
    }

    @Override
    public void setStartTimeInSecs(long startTimeInSecs) {
        this.serializedStartSecs = startTimeInSecs;
    }

    @Override
    public long getStartTimeInSeconds() {
        return serializedStartSecs;
    }

    @Override
    public long getEndTimeInSecs() {
        return serializedEndSecs;
    }

    @Override
    public void setEndTimeInSecs(long endTimeInSecs) {
        this.serializedEndSecs = endTimeInSecs;
    }

    public ServiceColor getServiceColor() {
        return serviceColor;
    }

    public void setServiceColor(ServiceColor serviceColor) {
        this.serviceColor = serviceColor;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int freq) {
        frequency = freq;
    }

    public boolean isFrequencyBased() {
        return frequency > 0;
    }

    @Override
    public String getStartStopCode() {
        return stopCode;
    }

    @Override
    public void setStartStopCode(String startStopCode) {
        stopCode = startStopCode;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void isFavourite(boolean isFavourite) {
        this.isFavourite = isFavourite;
    }

    public ArrayList<RealtimeAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(ArrayList<RealtimeAlert> alerts) {
        this.alerts = alerts;
    }

    public boolean hasAlerts() {
        return alerts != null && !alerts.isEmpty();
    }

    @Override
    public String getEndStopCode() {
        return endStopCode;
    }

    @Override
    public void setEndStopCode(String endStopCode) {
        this.endStopCode = endStopCode;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * In secs.
     */
    public long getServiceTime() {
        return serviceTime;
    }

    public void setServiceTime(long serviceTime) {
        this.serviceTime = serviceTime;
    }

    @Nullable
    public ArrayList<Long> getAlertHashCodes() {
        return alertHashCodes;
    }

    public void setAlertHashCodes(@Nullable ArrayList<Long> alertHashCodes) {
        this.alertHashCodes = alertHashCodes;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(stopCode);
        out.writeString(serviceTripId);
        out.writeString(serviceNumber);
        out.writeString(serviceName);
        out.writeString(realTimeStatus != null ? realTimeStatus.name() : null);
        out.writeLong(serializedStartSecs);
        out.writeLong(serializedEndSecs);
        out.writeParcelable(serviceColor, 0);
        out.writeInt(frequency);
        out.writeInt(isFavourite ? 1 : 0);
        out.writeList(alerts);
        out.writeString(searchString);
        out.writeString(endStopCode);
        out.writeParcelable(startStop, 0);
        out.writeParcelable(endStop, 0);
        out.writeString(mode == null ? null : mode.toString());
        out.writeString(pairIdentifier);
        out.writeString(operator);
        out.writeParcelable(realtimeVehicle, 0);
        out.writeLong(serviceTime);
        out.writeParcelable(modeInfo, 0);
        out.writeString(serviceDirection);
        out.writeValue(wheelchairAccessible);
        out.writeValue(bicycleAccessible);
        out.writeValue(startStopShortName);
        out.writeList(alertHashCodes);
        out.writeString(startPlatform);
    }

    @Deprecated
    public VehicleMode getMode() {
        return mode;
    }

    @Deprecated
    public void setMode(VehicleMode mode) {
        this.mode = mode;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * For example, in order to determine a past service trip.
     */
    public boolean isBefore(long pointSecs) {
        if (serializedEndSecs > 0) {
            return serializedEndSecs < pointSecs;
        } else {
            // Some services don't have arrival time.
            return serializedStartSecs < pointSecs;
        }
    }

    /**
     * For debug purpose only.
     */
    @NotNull
    @Override
    public String toString() {
        // Trim the package part to print out something less verbal.
        return TimetableEntry.class.getSimpleName() + hashCode();
    }

    public RealTimeVehicle getRealtimeVehicle() {
        return realtimeVehicle;
    }

    public void setRealtimeVehicle(RealTimeVehicle realtimeVehicle) {
        this.realtimeVehicle = realtimeVehicle;
    }

    @Nullable
    public Boolean getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(@Nullable Boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    @Nullable
    @Override
    public Boolean getBicycleAccessible() {
        return bicycleAccessible;
    }

    public void setBicycleAccessible(@Nullable Boolean bicycleAccessible) {
        this.bicycleAccessible = bicycleAccessible;
    }

    public String getStartStopShortName() {
        return startStopShortName;
    }

    public void setStartStopShortName(String startStopShortName) {
        this.startStopShortName = startStopShortName;
    }

    @Nullable
    public ModeInfo getModeInfo() {
        return modeInfo;
    }

    public void setModeInfo(@Nullable ModeInfo modeInfo) {
        this.modeInfo = modeInfo;
    }

    public int getRealTimeDeparture() {
        return realTimeDeparture;
    }

    public void setRealTimeDeparture(int realTimeDeparture) {
        this.realTimeDeparture = realTimeDeparture;
    }

    public int getRealTimeArrival() {
        return realTimeArrival;
    }

    public void setRealTimeArrival(int realTimeArrival) {
        this.realTimeArrival = realTimeArrival;
    }

    public String getStartPlatform() {
        return startPlatform;
    }

    public void setStartPlatform(String startPlatform) {
        this.startPlatform = startPlatform;
    }
}