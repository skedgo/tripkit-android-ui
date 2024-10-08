package com.skedgo.tripkit.ui.map.servicestop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus;
import com.skedgo.tripkit.common.model.stop.ScheduledStop;
import com.skedgo.tripkit.common.model.stop.ServiceStop;
import com.skedgo.tripkit.routing.ModeInfo;
import com.skedgo.tripkit.routing.VehicleMode;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.map.BearingMarkerIconBuilder;
import com.skedgo.tripkit.ui.map.MapMarkerUtils;
import com.skedgo.tripkit.ui.map.TimeLabelMaker;
import com.skedgo.tripkit.ui.model.StopInfo;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class ServiceStopMarkerCreator {
    private final Context context;
    private final TimeLabelMaker timeLabelMaker;

    public ServiceStopMarkerCreator(
        @NonNull Context context,
        @NonNull TimeLabelMaker timeLabelMaker) {
        this.context = context;
        this.timeLabelMaker = timeLabelMaker;
    }

    public static Drawable convert(@NonNull Context context,
                                   VehicleMode mode,
                                   RealTimeStatus realTimeStatus) {
        return mode != null
            ? (realTimeStatus == RealTimeStatus.IS_REAL_TIME) ? mode.getRealtimeMapIconRes(context) : mode.getMapIconRes(context)
            : ContextCompat.getDrawable(context, R.drawable.v4_ic_map_location);
    }

    public MarkerOptions toMarkerOptions(@NonNull StopInfo stopInfo, String displayText, String timeZone) {
        ServiceStop stop = stopInfo.stop;
        if (stop.getType() != null) {
            Pair<Bitmap, Float> icon = new BearingMarkerIconBuilder(context, timeLabelMaker)
                .hasBearing(true)
                .bearing(stop.getBearing())
                .vehicleIcon(convert(
                    context,
                    ScheduledStop.convertStopTypeToVehicleMode(stop.getType()),
                    stopInfo.realTimeStatus
                ))
                .vehicleIconScale(ModeInfo.MAP_LIST_SIZE_RATIO)
                .baseIcon(R.drawable.ic_map_pin_base)
                .pointerIcon(R.drawable.ic_map_pin_pointer)
                .hasBearingVehicleIcon(false)
                .hasTime(true)
                .time(TimeUnit.SECONDS.toMillis(stop.departureSecs()), timeZone)
                .build();

            return new MarkerOptions()
                .position(new LatLng(stop.getLat(), stop.getLon()))
                .draggable(false)
                .title(stop.getName())
                .snippet(displayText)
                .icon(BitmapDescriptorFactory.fromBitmap(icon.first))
                .anchor(icon.second, 1f)
                .infoWindowAnchor(icon.second, 0f);
        } else {
            int circleMarkerDiameter = (int) context.getResources().getDimension(R.dimen.stop_circle_pin_diameter);
            final int strokeColor = stopInfo.travelled ? stopInfo.serviceColor : Color.GRAY;
            final int fillColor = stopInfo.travelled ? Color.WHITE : Color.LTGRAY;
            final Bitmap icon = MapMarkerUtils.createStopMarkerIcon(
                circleMarkerDiameter,
                strokeColor,
                fillColor,
                !stopInfo.travelled);
            return new MarkerOptions()
                .position(new LatLng(stop.getLat(), stop.getLon()))
                .draggable(false)
                .title(stop.getName())
                .snippet(displayText)
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .anchor(0.5f, 0.5f)
                .infoWindowAnchor(0.5f, 0f);
        }
    }
}