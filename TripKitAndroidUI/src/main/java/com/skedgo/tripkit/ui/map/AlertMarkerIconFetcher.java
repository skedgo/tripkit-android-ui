package com.skedgo.tripkit.ui.map;

import android.content.res.Resources;

import com.google.android.gms.maps.model.Marker;
import com.skedgo.tripkit.common.model.RealtimeAlert;
import com.skedgo.tripkit.configuration.ServerManager;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import dagger.Lazy;

import static com.skedgo.tripkit.ui.map.IconUtils.asUrl;


public class AlertMarkerIconFetcher {
    private static final String URL_TEMPLATE = ServerManager.INSTANCE.getConfiguration().getApiTripGoUrl() + "modeicons/android/%s/ic_alert_%s.png";
    private final Resources resources;
    private final Lazy<Picasso> picassoLazy;

    @Inject
    AlertMarkerIconFetcher(
        Resources resources,
        Lazy<Picasso> picassoLazy) {
        this.resources = resources;
        this.picassoLazy = picassoLazy;
    }

    public void call(Marker marker, @NonNull RealtimeAlert realtimeAlert) {
        final String iconName = realtimeAlert.remoteIcon();
        if (iconName != null) {
            picassoLazy.get().load(asUrl(resources, iconName, URL_TEMPLATE))
                .into(new MarkerTarget(new WeakReference<>(marker)));
        }
    }
}
