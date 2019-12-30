package com.skedgo.tripkit.ui.map.adapter;

import android.view.View;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class SimpleInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
  @Override
  public View getInfoWindow(Marker marker) {
    // Fall back to default implementation.
    return null;
  }

  @Override
  public View getInfoContents(Marker marker) {
    return null;
  }
}