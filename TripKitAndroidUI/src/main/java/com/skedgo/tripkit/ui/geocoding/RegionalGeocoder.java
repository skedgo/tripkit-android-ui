package com.skedgo.tripkit.ui.geocoding;

import com.skedgo.tripkit.common.model.region.Region;
import com.skedgo.tripkit.ui.TripKitUI;

import java.util.List;

public class RegionalGeocoder extends Geocoder {
    public RegionalGeocoder() {
    }

    @Override
    public String getServiceUrl() {
        double latitude = getNearLatitude();
        double longitude = getNearLongitude();
        final Region r = TripKitUI.getInstance().regionService()
            .getRegionByLocationAsync(latitude, longitude)
            .blockingFirst();
        final List<String> urls = r.getURLs();
        return urls.get(0);
    }
}
