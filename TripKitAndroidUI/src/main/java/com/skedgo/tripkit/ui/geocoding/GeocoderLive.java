package com.skedgo.tripkit.ui.geocoding;

import androidx.core.util.Pair;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.ui.utils.HttpUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class GeocoderLive extends RegionalGeocoder {
  private static final String PARAM_AUTOCOMPLETE = "a";
  private static final int ON_VALUE = 1;

  @Inject GeocoderLive() {}

  public double getNearLatitude() {
    return mNearLatitude;
  }

  public GeocoderLive setNearLatitude(double nearLatitude) {
    mNearLatitude = nearLatitude;
    return this;
  }

  public double getNearLongitude() {
    return mNearLongitude;
  }

  public GeocoderLive setNearLongitude(double nearLongitude) {
    mNearLongitude = nearLongitude;
    return this;
  }

  public List<Location> query(String query) throws IOException {
    List<Pair<String, Object>> params = asParams(query);
    params.add(new Pair<>(PARAM_AUTOCOMPLETE, ON_VALUE));

    String response = HttpUtils.get(getServiceUrl() + GEOCODE_METHOD, params);
    GeocodeResponse geocodeResponse = mGson.fromJson(response, GeocodeResponse.class);
    if (geocodeResponse == null) {
      return null;
    } else {
      return geocodeResponse.getChoiceList();
    }
  }
}
