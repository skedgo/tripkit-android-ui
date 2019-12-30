package com.skedgo.tripkit.ui.geocoding;
import com.skedgo.tripkit.ui.data.places.LatLngBounds;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
public interface CitySelectedEvent {
  @Value.Parameter
  LatLngBounds bounds();
}