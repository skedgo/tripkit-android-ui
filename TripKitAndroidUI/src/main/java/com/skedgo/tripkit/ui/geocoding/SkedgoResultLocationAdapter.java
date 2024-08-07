package com.skedgo.tripkit.ui.geocoding;

import com.skedgo.geocoding.agregator.GCSkedGoResultInterface;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.ui.data.places.Place;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SkedgoResultLocationAdapter implements GCSkedGoResultInterface, ResultLocationAdapter<Place.TripGoPOI> {
    private final Location location;
    private final GCSkedGoResultInterface resultInterface;

    public SkedgoResultLocationAdapter(Location location, GCSkedGoResultInterface resultInterface) {
        this.location = location;
        this.resultInterface = resultInterface;
    }

    @Override
    public Place.TripGoPOI getPlace() {
        return new Place.TripGoPOI(location);
    }

    @NonNull
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

    @Override
    public String getResultClass() {
        return resultInterface.getResultClass();
    }

    @Override
    public int getPopularity() {
        return resultInterface.getPopularity();
    }

    @Nullable
    @Override
    public List<String> getModeIdentifiers() {
        return resultInterface.getModeIdentifiers();
    }
}