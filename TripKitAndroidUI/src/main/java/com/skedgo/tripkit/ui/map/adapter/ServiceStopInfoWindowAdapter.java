package com.skedgo.tripkit.ui.map.adapter;

import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.maps.model.Marker;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.map.SimpleCalloutView;

import androidx.annotation.NonNull;

public final class ServiceStopInfoWindowAdapter extends SimpleInfoWindowAdapter {
    private final LayoutInflater inflater;

    public ServiceStopInfoWindowAdapter(@NonNull LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public View getInfoContents(Marker marker) {
        final SimpleCalloutView view = SimpleCalloutView.create(inflater);
        view.setTitle(marker.getTitle());
        view.setSnippet(marker.getSnippet());
        view.setRightImage(R.drawable.ic_arrow_forward);
        return view;
    }
}