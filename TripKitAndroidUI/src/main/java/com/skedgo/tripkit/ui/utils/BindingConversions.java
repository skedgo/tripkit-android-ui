package com.skedgo.tripkit.ui.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.databinding.BindingConversion;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.StopType;
import com.skedgo.tripkit.ui.R;

public final class BindingConversions {
  private BindingConversions() {}

  @DrawableRes
  public static int convertStopTypeToMapIconRes(@Nullable StopType stopType) {
    if (stopType == StopType.BUS) {
      return R.drawable.ic_map_stop_bus;
    } else if (stopType == StopType.TRAIN) {
      return R.drawable.ic_map_stop_train;
    } else if (stopType == StopType.FERRY) {
      return R.drawable.ic_map_stop_ferry;
    } else if (stopType == StopType.MONORAIL) {
      return R.drawable.ic_map_stop_monorail;
    } else if (stopType == StopType.SUBWAY) {
      return R.drawable.ic_map_stop_subway;
    } else if (stopType == StopType.TAXI) {
      return R.drawable.ic_map_stop_taxi;
    } else if (stopType == StopType.PARKING) {
      return R.drawable.ic_map_stop_parking;
    } else if (stopType == StopType.TRAM) {
      return R.drawable.ic_map_stop_tram;
    } else if (stopType == StopType.CABLECAR) {
      return R.drawable.ic_map_stop_cablecar;
    } else {
      return 0;
    }
  }

  @BindingConversion
  public static int convertLocationToAlpha(Location location) {
    return location == null ? 1 : 0;
  }
}
