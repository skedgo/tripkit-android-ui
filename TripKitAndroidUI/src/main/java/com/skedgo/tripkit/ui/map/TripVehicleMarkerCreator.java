package com.skedgo.tripkit.ui.map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.skedgo.tripkit.common.util.DateTimeFormats;
import com.skedgo.tripkit.common.util.StringUtils;
import com.skedgo.tripkit.ui.R;
import dagger.Lazy;
import com.skedgo.tripkit.routing.RealTimeVehicle;
import com.skedgo.tripkit.routing.TripSegment;
import com.skedgo.tripkit.routing.VehicleMode;

import javax.inject.Inject;

public class TripVehicleMarkerCreator {
  private final Context context;
  private final Lazy<VehicleMarkerIconCreator> vehicleMarkerIconCreatorLazy;

  @Inject TripVehicleMarkerCreator(
      Context context,
      Lazy<VehicleMarkerIconCreator> vehicleMarkerIconCreatorLazy) {
    this.context = context;
    this.vehicleMarkerIconCreatorLazy = vehicleMarkerIconCreatorLazy;
  }

  public MarkerOptions call(Resources resources, TripSegment segment) {
    final RealTimeVehicle vehicle = segment.getRealTimeVehicle();
    final long millis = vehicle.getLastUpdateTime() * 1000;
    final String time = DateTimeFormats.printTime(context, millis, segment.getTimeZone());
    String title = null;
    String snippet;
    if (segment.getMode() == VehicleMode.BUS) {
      if (TextUtils.isEmpty(segment.getServiceNumber())) {
        title = "Your upcoming service"; // TODO: i18n
      } else {
        if (segment.getMode() != null && segment.getMode().isPublicTransport()) {
          title = StringUtils.capitalizeFirst(segment.getMode().toString()) + " " + segment.getServiceNumber();
        }

        if (TextUtils.isEmpty(title)) {
          title = "Service " + segment.getServiceNumber();
        }
      }

      snippet = (TextUtils.isEmpty(vehicle.getLabel())
          ? "Real-time"
          : "Vehicle " + vehicle.getLabel()) + " location as at " + time;
    } else {
      title = StringUtils.firstNonEmpty(
          segment.getServiceName(),
          vehicle.getLabel(),
          "Your upcoming service"
      );
      snippet = (segment.getMode() == null
          ? "Location"
          : StringUtils.capitalizeFirst(segment.getMode().toString()) + " location")
          + " as at " + time;
    }

    final int bearing = vehicle.getLocation() == null
        ? 0
        : vehicle.getLocation().getBearing();
    final int color = segment.getServiceColor() == null || segment.getServiceColor().getColor() == Color.BLACK
        ? resources.getColor(R.color.v4_color)
        : segment.getServiceColor().getColor();

    final String text = TextUtils.isEmpty(segment.getServiceNumber())
        ? (segment.getMode() == null ? "" : StringUtils.capitalizeFirst(segment.getMode().toString()))
        : segment.getServiceNumber();

    final Bitmap icon = vehicleMarkerIconCreatorLazy.get().call(bearing, color, text);
    return new MarkerOptions()
        .icon(BitmapDescriptorFactory.fromBitmap(icon))
        .rotation(bearing)
        .flat(true)
        .anchor(0.5f, 0.5f)
        .title(title)
        .snippet(snippet)
        .position(new LatLng(
            vehicle.getLocation().getLat(),
            vehicle.getLocation().getLon()
        ))
        .draggable(false);
  }
}