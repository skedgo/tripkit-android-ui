package com.skedgo.tripkit.ui.geocoding;

import com.skedgo.tripkit.common.model.Location;
import com.skedgo.geocoding.agregator.GCAppResultInterface;
import com.skedgo.tripkit.ui.data.places.Place;
import org.jetbrains.annotations.NotNull;

public final class AppResultLocationAdapter implements ResultLocationAdapter<Place.TripGoPOI>, GCAppResultInterface {
  private final GCAppResultInterface resultInterface;
  private final Location location;

  public AppResultLocationAdapter(Location location, GCAppResultInterface resultInterface) {
    this.location = location;
    this.resultInterface = resultInterface;
  }

  @Override
  public Place.TripGoPOI getPlace() {
    return new Place.TripGoPOI(location);
  }

  @Override public String getSubtitle() {
    return resultInterface.getSubtitle();
  }

  @Override public GCAppResultInterface.Source getAppResultSource() {
    return resultInterface.getAppResultSource();
  }

  @Override public boolean isFavourite() {
    return resultInterface.isFavourite();
  }

  @NotNull
  @Override public String getName() {
    return resultInterface.getName();
  }

  @Override public Double getLat() {
    return resultInterface.getLat();
  }

  @Override public Double getLng() {
    return resultInterface.getLng();
  }
}