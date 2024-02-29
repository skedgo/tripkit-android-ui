package com.skedgo.tripkit.ui.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.configuration.ServerManager;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import dagger.Lazy;
import com.skedgo.tripkit.routing.RealTimeVehicle;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class VehicleMarkerIconFetcher {
  private static final String URL_TEMPLATE = ServerManager.INSTANCE.getConfiguration().getApiTripGoUrl() + "modeicons/android/%s/ic_vehicle_%s.png";
  private final Resources resources;
  private final Lazy<Picasso> picassoLazy;

  @Inject VehicleMarkerIconFetcher(
      Resources resources,
      Lazy<Picasso> picassoLazy) {
    this.resources = resources;
    this.picassoLazy = picassoLazy;
  }

  public void call(Marker marker, @NonNull RealTimeVehicle vehicle) {
    final String icon = vehicle.getIcon();
    if (icon != null) {
      // If a marker was removed from a map, mutating its icon is unnecessary.
      final WeakReference<Marker> markerWeakReference = new WeakReference<>(marker);
      picassoLazy.get().load(IconUtils.asUrl(resources, icon, URL_TEMPLATE))
          .into(new Target() {
            @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
              try {
                final Marker actualMarker = markerWeakReference.get();
                if (actualMarker != null) {
                  actualMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));

                  // By default, the icon provided by server is rotated
                  // to the left by 90 degrees.
                  // So we gotta plus 90 to make it North aligned again.
                  final Location location = vehicle.getLocation();
                  final int bearing = location != null ? location.getBearing() : 0;
                  actualMarker.setRotation(bearing + 90);
                }
              } catch (Exception e) {
              }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

            }
            @Override public void onPrepareLoad(Drawable placeHolderDrawable) {}
          });
    }
  }
}