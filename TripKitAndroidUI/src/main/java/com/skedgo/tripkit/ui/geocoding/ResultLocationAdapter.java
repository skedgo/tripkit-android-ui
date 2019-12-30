package com.skedgo.tripkit.ui.geocoding;

import com.skedgo.tripkit.ui.data.places.Place;

public interface ResultLocationAdapter<T extends Place> {
  T getPlace();
}