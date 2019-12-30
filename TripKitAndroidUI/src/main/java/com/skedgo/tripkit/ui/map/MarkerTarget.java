package com.skedgo.tripkit.ui.map;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.ref.WeakReference;

public class MarkerTarget implements Target {
  /**
   * If a marker was removed from a map, mutating
   * its icon is unnecessary, and
   * we should let it be GC-ed quickly as possible.
   * That's why a weak reference is used.
   */
  private final WeakReference<Marker> markerWeakReference;

  public MarkerTarget(WeakReference<Marker> markerWeakReference) {
    this.markerWeakReference = markerWeakReference;
  }

  @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
    try {
      final Marker actualMarker = markerWeakReference.get();
      if (actualMarker != null) {
        actualMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
      }
    } catch (Exception e) {
    }
  }

  @Override
  public void onBitmapFailed(Exception e, Drawable errorDrawable) {

  }

  @Override public void onPrepareLoad(Drawable placeHolderDrawable) {}
}