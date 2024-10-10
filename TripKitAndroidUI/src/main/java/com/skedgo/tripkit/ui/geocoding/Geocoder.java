package com.skedgo.tripkit.ui.geocoding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skedgo.tripkit.common.model.location.Location;
import com.skedgo.tripkit.common.util.Gsons;
import com.skedgo.tripkit.ui.utils.HttpUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Pair;

public class Geocoder {
    protected static final String PARAM_QUERY = "q";
    protected static final String PARAM_NEAR = "near";
    protected static final String PARAM_ALLOW_YELP = "allowYelp";
    protected static final String PARAM_ALLOW_GOOGLE = "allowGoogle"; //allowGoogle=false
    protected static final String GEOCODE_METHOD = "/geocode.json";
    protected double mNearLatitude = Double.MAX_VALUE;
    protected double mNearLongitude = Double.MAX_VALUE;
    protected Gson mGson;
    private String mServiceUrl;
    private boolean mAllowGoogle;

    public Geocoder() {
        mGson = new GsonBuilder()
            .registerTypeAdapter(Location.class, new GeocodeResultAdapter(Gsons.createForLowercaseEnum()))
            .create();
    }

    public double getNearLatitude() {
        return mNearLatitude;
    }

    public Geocoder setNearLatitude(double nearLatitude) {
        mNearLatitude = nearLatitude;
        return this;
    }

    public double getNearLongitude() {
        return mNearLongitude;
    }

    public Geocoder setNearLongitude(double nearLongitude) {
        mNearLongitude = nearLongitude;
        return this;
    }

    public String getServiceUrl() {
        return mServiceUrl;
    }

    public List<Location> query(String query) throws IOException {
        String response = HttpUtils.get(getServiceUrl() + GEOCODE_METHOD, asParams(query));
        GeocodeResponse geocodeResponse = mGson.fromJson(response, GeocodeResponse.class);
        if (geocodeResponse == null) {
            return null;
        } else {
            return geocodeResponse.getChoiceList();
        }
    }

    protected List<Pair<String, Object>> asParams(String query) {
        List<Pair<String, Object>> params = new ArrayList<>();
        params.add(new Pair<>(PARAM_QUERY, query));
        params.add(new Pair<>(PARAM_ALLOW_YELP, true));
        params.add(new Pair<>(PARAM_ALLOW_GOOGLE, mAllowGoogle));
        if (Double.compare(mNearLatitude, Double.MAX_VALUE) != 0
            && Double.compare(mNearLongitude, Double.MAX_VALUE) != 0) {
            String nearAddress = "(" + mNearLatitude + "," + mNearLongitude + ")";
            params.add(new Pair<>(PARAM_NEAR, nearAddress));
        }
        return params;
    }
}
