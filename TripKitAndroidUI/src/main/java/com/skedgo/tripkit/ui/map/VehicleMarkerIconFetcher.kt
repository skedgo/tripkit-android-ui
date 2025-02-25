package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.skedgo.rxtry.printThrowableStackTrace
import com.skedgo.tripkit.configuration.ServerManager.configuration
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.map.IconUtils.asUrl
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import dagger.Lazy
import java.lang.ref.WeakReference
import javax.inject.Inject

class VehicleMarkerIconFetcher @Inject internal constructor(
    private val resources: Resources,
    private val picassoLazy: Lazy<Picasso>
) {
    fun call(marker: Marker, vehicle: RealTimeVehicle) {
        val icon = vehicle.icon
        if (icon != null) {
            // If a marker was removed from a map, mutating its icon is unnecessary.
            val markerWeakReference = WeakReference(marker)
            picassoLazy.get().load(asUrl(resources, icon, URL_TEMPLATE))
                .into(object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                        try {
                            val actualMarker = markerWeakReference.get()
                            if (actualMarker != null) {
                                actualMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))

                                // By default, the icon provided by server is rotated
                                // to the left by 90 degrees.
                                // So we gotta plus 90 to make it North aligned again.
                                val location = vehicle.location
                                val bearing = location?.bearing ?: 0
                                actualMarker.rotation = (bearing + 90).toFloat()
                            }
                        } catch (e: Exception) {
                            e.printThrowableStackTrace()
                        }
                    }

                    override fun onBitmapFailed(e: Exception, errorDrawable: Drawable) {
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable) {
                    }
                })
        }
    }

    companion object {
        private val URL_TEMPLATE =
            configuration.staticTripGoUrl + "icons/android/%s/ic_vehicle_%s.png"
    }
}