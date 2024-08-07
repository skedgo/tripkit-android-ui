package com.skedgo.tripkit.ui.map;

import android.content.res.Resources;

import com.google.android.gms.maps.model.Marker;
import com.skedgo.tripkit.routing.ModeInfo;
import com.skedgo.tripkit.ui.utils.StopMarkerUtils;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class StopMarkerIconFetcher {
    private final Resources resources;
    private final Picasso picasso;

    @Inject
    public StopMarkerIconFetcher(
        @NonNull Resources resources,
        @NonNull Picasso picasso) {
        this.resources = resources;
        this.picasso = picasso;
    }

    public void call(@NonNull final Marker marker, @Nullable ModeInfo modeInfo) {
        if (modeInfo != null) {
            final String url = StopMarkerUtils.INSTANCE.getMapIconUrlForModeInfo(resources, modeInfo);
            picasso.load(url).into(new MarkerTarget(new WeakReference<>(marker)));
        }
    }
}