package com.skedgo.tripkit.ui.tripresults;

import android.content.Context;
import android.content.res.Resources;

import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.RealtimeAlert;
import com.skedgo.tripkit.common.model.RealtimeAlerts;
import com.skedgo.tripkit.common.util.TransportModeUtils;
import com.skedgo.tripkit.common.util.TripSegmentUtils;
import com.skedgo.tripkit.routing.ModeInfo;
import com.skedgo.tripkit.routing.TripSegment;

import javax.inject.Inject;

public class TripSegmentHelper {
    @Inject
    public TripSegmentHelper() {
    }

    public String getRealTimeAlertDisplayText(RealtimeAlert alert) {
        return RealtimeAlerts.getDisplayText(alert);
    }

    public String getIconUrlForModeInfo(Resources resources, ModeInfo modeInfo) {
        return TransportModeUtils.getIconUrlForModeInfo(resources, modeInfo);
    }

    public String getTripSegmentAction(Context context, TripSegment tripSegment) {
        return TripSegmentUtils.getTripSegmentAction(context, tripSegment);
    }

    String getLocationName(Location to) {
        return TripSegmentUtils.getLocationName(to);
    }
}