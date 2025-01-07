package com.skedgo.tripkit.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.utils.DeviceInfo
import com.skedgo.tripkit.ui.utils.StopMarkerUtils.getStaticMapIconUrlForModeInfo
import com.squareup.picasso.Picasso
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.lang.ref.WeakReference
import javax.inject.Inject

class RemoteMarkerIconFetcher @Inject constructor(
    private val picasso: Picasso
) {

    companion object {
        const val SIZE_CIRCULAR_BITMAP = 60
        const val TINT_BITMAP_RGB = 255
    }

    fun call(markerOptions: MarkerOptions, modeInfo: ModeInfo?) {
        modeInfo?.let {
            val url = getStaticMapIconUrlForModeInfo(DeviceInfo.getDensityDpiName(), it)
            picasso.load(url).into(MarkerOptionsTarget(WeakReference(markerOptions)))
        }
    }

    fun callAsync(markerOptions: MarkerOptions, modeInfo: ModeInfo?): Single<MarkerOptions> {
        val iconUrl = getStaticMapIconUrlForModeInfo(DeviceInfo.getDensityDpiName(), modeInfo)
        return Single.defer {
            Single.create { emitter ->
                picasso.load(iconUrl)
                    .into(object : com.squareup.picasso.Target {
                        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                            bitmap?.let {
                                val circularBitmap = createCircularMarkerBitmap(
                                    it,
                                    Color.rgb(TINT_BITMAP_RGB, TINT_BITMAP_RGB, TINT_BITMAP_RGB),
                                    Color.rgb(
                                        modeInfo?.color?.red ?: 0,
                                        modeInfo?.color?.green ?: 0,
                                        modeInfo?.color?.blue ?: 0
                                    ),
                                    SIZE_CIRCULAR_BITMAP
                                )

                                val scaledBitmap = Bitmap.createScaledBitmap(
                                    circularBitmap,
                                    SIZE_CIRCULAR_BITMAP, SIZE_CIRCULAR_BITMAP, false
                                )

                                val icon = BitmapDescriptorFactory.fromBitmap(scaledBitmap)
                                markerOptions.icon(icon)
                                emitter.onSuccess(markerOptions)
                            } ?: run {
                                emitter.onError(Throwable("Bitmap is null"))
                            }
                        }

                        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                            e?.printStackTrace()
                            val fallbackIcon =
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                            markerOptions.icon(fallbackIcon)
                            emitter.onSuccess(markerOptions)
                        }

                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                            // Placeholder if needed
                        }
                    })
            }
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun createCircularMarkerBitmap(
        bitmap: Bitmap,
        tintColor: Int,
        circleColor: Int,
        circleRadius: Int
    ): Bitmap {
        val output = Bitmap.createBitmap(circleRadius * 2, circleRadius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw the circular background
        val paint = Paint().apply {
            isAntiAlias = true
            color = circleColor
        }
        canvas.drawCircle(circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat(), paint)

        // Apply tint to the bitmap
        val tintedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val bitmapCanvas = Canvas(tintedBitmap)
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tintColor, SRC_IN)
        }
        bitmapCanvas.drawBitmap(tintedBitmap, 0f, 0f, tintPaint)

        // Draw the tinted bitmap onto the circular background
        val scaledBitmap = Bitmap.createScaledBitmap(
            tintedBitmap,
            circleRadius,
            circleRadius,
            true
        )
        val left = (output.width - scaledBitmap.width) / 2f
        val top = (output.height - scaledBitmap.height) / 2f
        canvas.drawBitmap(scaledBitmap, left, top, null)

        return output
    }
}