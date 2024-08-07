package com.skedgo.tripkit.ui.map.adapter;

import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.maps.model.Marker;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.map.SimpleCalloutView;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ViewableInfoWindowAdapter implements StopInfoWindowAdapter {
    private final LayoutInflater inflater;

    @Nullable
    private SimpleCalloutView view;

    @Inject
    public ViewableInfoWindowAdapter(@NonNull LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public View getInfoContents(Marker marker) {
        if (view == null) {
            view = SimpleCalloutView.create(inflater);
        }
        assert view != null;
        view.setTitle(marker.getTitle());
        view.setSnippet(marker.getSnippet());
        view.setRightImage(R.drawable.ic_arrow_forward);
        return view;
    }

    @Override
    public int windowInfoHeightInPixel(@NotNull Marker marker) {
        if (view != null) {
            return view.getHeight();
        } else {
            return 0;
        }
    }

    @Override
    public void onInfoWindowClosed(@NotNull Marker marker) {
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

}