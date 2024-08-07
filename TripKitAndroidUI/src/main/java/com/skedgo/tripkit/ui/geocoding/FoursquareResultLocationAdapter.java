package com.skedgo.tripkit.ui.geocoding;

import com.skedgo.geocoding.agregator.GCFoursquareResultInterface;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.ui.data.places.Place;

import java.util.List;

public final class FoursquareResultLocationAdapter implements GCFoursquareResultInterface, ResultLocationAdapter<Place.TripGoPOI> {
    private final GCFoursquareResultInterface resultInterface;
    private final Location location;

    public FoursquareResultLocationAdapter(Location location, GCFoursquareResultInterface resultInterface) {
        this.location = location;
        this.resultInterface = resultInterface;
    }

    @Override
    public Place.TripGoPOI getPlace() {
        return new Place.TripGoPOI(location);
    }

    @Override
    public boolean isVerified() {
        return resultInterface.isVerified();
    }

    @Override
    public List<String> getCategories() {
        return resultInterface.getCategories();
    }

    @Override
    public String getName() {
        return resultInterface.getName();
    }

    @Override
    public Double getLat() {
        return resultInterface.getLat();
    }

    @Override
    public Double getLng() {
        return resultInterface.getLng();
    }
}