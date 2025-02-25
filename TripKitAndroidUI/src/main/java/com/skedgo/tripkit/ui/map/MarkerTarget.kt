package com.skedgo.tripkit.ui.map

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.skedgo.rxtry.printThrowableStackTrace
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.lang.ref.WeakReference

class MarkerTarget(
    /**
     * If a marker was removed from a map, mutating
     * its icon is unnecessary, and
     * we should let it be GC-ed quickly as possible.
     * That's why a weak reference is used.
     */
    private val markerWeakReference: WeakReference<Marker>
) : Target {
    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
        try {
            val actualMarker = markerWeakReference.get()
            actualMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
        } catch (e: Exception) {
            e.printThrowableStackTrace()
        }
    }

    override fun onBitmapFailed(e: Exception, errorDrawable: Drawable) {
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable) {
    }
}