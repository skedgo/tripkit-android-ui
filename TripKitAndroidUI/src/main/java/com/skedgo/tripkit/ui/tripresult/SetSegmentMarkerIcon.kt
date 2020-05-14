package com.skedgo.tripkit.ui.tripresult
import android.graphics.Bitmap
import android.util.Pair
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.rxtry.Try
import io.reactivex.functions.Consumer
import timber.log.Timber
import java.lang.ref.WeakReference

internal class SetSegmentMarkerIcon(segmentMarker: Marker) : Consumer<Try<Pair<Bitmap, Float>>> {
  private val segmentMarkerRef: WeakReference<Marker> = WeakReference(segmentMarker)

  override fun accept(result: Try<Pair<Bitmap, Float>>) {
    try {
      when (result) {
        is Success<Pair<Bitmap, Float>> -> {
           segmentMarkerRef.get()?.setIcon(BitmapDescriptorFactory.fromBitmap(result().first))
          segmentMarkerRef.get()?.setAnchor(result().second, 1.0f)
        }
        is Failure<Pair<Bitmap, Float>> -> Timber.e(result())
      }
    } catch (e: Exception) {
      // Because there would be an exception thrown if setting icon for
      // a marker which was removed from the map.
      Timber.e(e)
    }
  }
}
