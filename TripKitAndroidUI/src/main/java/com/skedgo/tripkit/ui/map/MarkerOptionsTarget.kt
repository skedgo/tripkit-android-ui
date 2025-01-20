package com.skedgo.tripkit.ui.map

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.lang.ref.WeakReference

class MarkerOptionsTarget(
    /**
     * If a marker was removed from a map, mutating
     * its icon is unnecessary, and
     * we should let it be GC-ed quickly as possible.
     * That's why a weak reference is used.
     */
    private val markerOptionsWeakReference: WeakReference<MarkerOptions>
) : Target {

    override fun onBitmapLoaded(bitmap: Bitmap?, from: LoadedFrom?) {
        bitmap?.let {
            val actualMarkerOptions = markerOptionsWeakReference.get()
            val icon = BitmapDescriptorFactory.fromBitmap(it)
            actualMarkerOptions?.icon(icon)
        } ?: run {
            println("bitmap is null")
        }
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        val actualMarkerOptions = markerOptionsWeakReference.get()
        val fallbackIcon =
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        actualMarkerOptions?.icon(fallbackIcon)
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        // Placeholder if needed
    }
}