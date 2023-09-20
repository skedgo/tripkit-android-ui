package com.skedgo.tripkit.ui.timetables;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.RealTimeStatus;
import com.skedgo.tripkit.common.model.ScheduledStop;
import com.skedgo.tripkit.common.model.ServiceStop;
import com.skedgo.tripkit.ui.model.StopInfo;
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

public class LoadServiceTask implements Callable<Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>>> {
    private static final String TAG = "LoadServiceTask";

    private final Cursor mCursor;
    private ScheduledStop mStop;
    private SparseArray<Pair<Integer, List<LatLng>>> mIdToLatLngArray = new SparseArray<>();

    public LoadServiceTask(ScheduledStop stop, Cursor cursor) {
        mStop = stop;
        mCursor = cursor;
    }

    public ScheduledStop getStopFor(final String code) {
        if (mStop != null) {
            if (TextUtils.equals(mStop.getCode(), code)) {
                return mStop;
            } else if (mStop.hasChildren()) {
                for (ScheduledStop stop : mStop.getChildren()) {
                    if (TextUtils.equals(stop.getCode(), code)) {
                        return stop;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>> call() {
        if (mCursor == null || mCursor.getCount() == 0) {
            return new Pair<>(Collections.emptyList(), Collections.emptyList());
        }
        final List<StopInfo> stopInfoList = new ArrayList<>();
        if (LoadServiceTaskCursorCols.id < 0) {
            LoadServiceTaskCursorCols.init(mCursor);
        }
        final HashSet<String> alreadyAddedStopCodes = new HashSet<>();
        for (int i = 0; i < mCursor.getCount(); i++) {
            mCursor.moveToPosition(i);
            final String stopCode = mCursor.getString(LoadServiceTaskCursorCols.stopCode);

            // Defend against duplicate issue.
            if (alreadyAddedStopCodes.contains(stopCode)) {
                continue;
            } else {
                alreadyAddedStopCodes.add(stopCode);
            }

            final int id = mCursor.getInt(LoadServiceTaskCursorCols.id);

            // FIXME: Wiki said service.json did not return address but only name.
            final String address = mCursor.getString(LoadServiceTaskCursorCols.address);
            final String name = mCursor.getString(LoadServiceTaskCursorCols.name);
            int bearing = mCursor.getInt(LoadServiceTaskCursorCols.bearing);
            final RealTimeStatus realTimeStatus = RealTimeStatus.from(mCursor.getString(LoadServiceTaskCursorCols.realTimeStatus));
            final long depart = mCursor.getLong(LoadServiceTaskCursorCols.departureTime);
            final long arrive = mCursor.getLong(LoadServiceTaskCursorCols.arrivalTime);
            final int color = mCursor.getInt(LoadServiceTaskCursorCols.serviceColor);

            double latitude = mCursor.getDouble(LoadServiceTaskCursorCols.lat);
            double longitude = mCursor.getDouble(LoadServiceTaskCursorCols.lon);

            int wheelchairAccessible = mCursor.getInt(LoadServiceTaskCursorCols.wheelchairAccessible);

            ServiceStop stop = new ServiceStop();
            stop.setLat(latitude);
            stop.setLon(longitude);
            stop.setBearing(bearing);
            stop.setName(name);
            stop.setAddress(address);
            stop.setCode(stopCode);
            stop.setDepartureSecs(depart);
            stop.setArrivalTime(arrive);
            if (wheelchairAccessible != -1) {
                stop.setWheelchairAccessible(wheelchairAccessible == 1 ? Boolean.TRUE : Boolean.FALSE);
            }

            boolean sortByArrive = true;
            if (mCursor.getPosition() == 0 && depart != 0) {
                sortByArrive = false;
            } else {
                if (depart != 0) {
                    sortByArrive = false;
                }
            }

            if (mStop != null && getStopFor(stopCode) != null) {
                stop.setType(mStop.getType());
            }

            if (mIdToLatLngArray.get(id) == null) {
                String waypointEncoding = mCursor.getString(LoadServiceTaskCursorCols.waypoints);
                if (waypointEncoding != null && !waypointEncoding.isEmpty()) {
                    mIdToLatLngArray.put(id, new Pair<Integer, List<LatLng>>(color, PolyUtil.decode(waypointEncoding)));
                }
            }

            stopInfoList.add(new StopInfo(id, realTimeStatus, sortByArrive, stop, color, false));
        }

        Collections.sort(stopInfoList, (lhs, rhs) -> {
            long val1 = lhs.sortByArrive ? lhs.stop.getArrivalTime() : lhs.stop.departureSecs();
            long val2 = rhs.sortByArrive ? rhs.stop.getArrivalTime() : rhs.stop.departureSecs();
            return (int) (val1 - val2);
        });
        calculateTravelledInfo(stopInfoList);
        final List<ServiceLineOverlayTask.ServiceLineInfo> serviceLineInfos = getServiceLineInfos();

        return new Pair<>(stopInfoList, serviceLineInfos);
    }

    @NotNull
    private List<ServiceLineOverlayTask.ServiceLineInfo> getServiceLineInfos() {
        boolean travelled = false;

        final List<ServiceLineOverlayTask.ServiceLineInfo> serviceLineInfos = new ArrayList<>();
        for (int i = 0; i < mIdToLatLngArray.size(); i++) {
            Pair<Integer, List<LatLng>> value = mIdToLatLngArray.valueAt(i);
            int index = -1;
            for (int j = 0; j < value.second.size(); j++) {
                LatLng latLng = value.second.get(j);
                if (new Location(latLng.latitude, latLng.longitude).distanceTo(mStop) < 10 /* in meters */) {
                    index = j;
                    travelled = true;
                    break;
                }
            }
            if (index == -1) {
                serviceLineInfos.add(new ServiceLineOverlayTask.ServiceLineInfo(value.second, value.first, travelled));
            } else {
                serviceLineInfos.add(new ServiceLineOverlayTask.ServiceLineInfo(value.second.subList(0, index + 1), value.first, false));
                serviceLineInfos.add(new ServiceLineOverlayTask.ServiceLineInfo(value.second.subList(index, value.second.size()), value.first, true));
            }
        }
        return serviceLineInfos;
    }

    private void calculateTravelledInfo(List<StopInfo> stopInfoList) {
        boolean travelled = false;
        for (StopInfo stopInfo : stopInfoList) {
            if (stopInfo.stop.getCode().equals(mStop.getCode())) {
                travelled = true;
            }
            stopInfo.travelled = travelled;
        }
    }

}