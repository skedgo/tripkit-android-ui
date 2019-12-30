package com.skedgo.tripkit.ui.map;

import android.text.TextUtils;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.skedgo.tripkit.common.model.Location;

import javax.inject.Inject;

public class TripLocationMarkerCreator {
  @Inject public TripLocationMarkerCreator() {}

  public MarkerOptions call(Location location) {
    String title = location.getName();
    String snippet = null;

    if (TextUtils.isEmpty(title)) {
      title = location.getAddress();
      if (TextUtils.isEmpty(title)) {
        title = location.getCoordinateString();
      }
    } else {
      snippet = location.getAddress();
    }

    LatLng markerPosition = new LatLng(location.getLat(), location.getLon());
    return new MarkerOptions()
        .title(title)
        .snippet(snippet)
        .draggable(false)
        .position(markerPosition);
  }
}
