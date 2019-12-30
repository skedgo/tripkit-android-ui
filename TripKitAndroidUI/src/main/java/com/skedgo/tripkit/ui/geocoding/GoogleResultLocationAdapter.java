package com.skedgo.tripkit.ui.geocoding;

import androidx.annotation.NonNull;
import com.skedgo.geocoding.agregator.GCGoogleResultInterface;
import com.skedgo.tripkit.ui.data.places.Place;

public final class GoogleResultLocationAdapter implements GCGoogleResultInterface, ResultLocationAdapter<Place.WithoutLocation> {
  private final GCGoogleResultInterface resultInterface;
  private Place.WithoutLocation location;

  public GoogleResultLocationAdapter(Place.WithoutLocation withoutLocation, GCGoogleResultInterface resultInterface) {
    this.location = withoutLocation;
    this.resultInterface = resultInterface;
  }

  @Override
  public Place.WithoutLocation getPlace() {
    return location;
  }

  @NonNull @Override public String getName() {
    return resultInterface.getName();
  }

  @Override public Double getLat() {
    return resultInterface.getLat();
  }

  @Override public Double getLng() {
    return resultInterface.getLng();
  }

  @Override public String getAddress() {
    return resultInterface.getAddress();
  }
}