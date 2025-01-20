package com.skedgo.tripkit.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.utils.BindingConversions
import com.skedgo.tripkit.ui.utils.DeviceInfo
import com.skedgo.tripkit.ui.utils.StopMarkerUtils.getLocalMapIconUrlForModeInfo
import com.skedgo.tripkit.ui.utils.StopMarkerUtils.getRemoteMapIconUrlForModeInfo
import com.squareup.picasso.Picasso
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.lang.ref.WeakReference
import javax.inject.Inject

class RemoteMarkerIconFetcher @Inject constructor(
    private val picasso: Picasso
) {

    companion object {
        const val SIZE_CIRCULAR_BITMAP = 30
        const val TINT_BITMAP_RGB = 255
    }

    fun call(markerOptions: MarkerOptions, modeInfo: ModeInfo?) {
        modeInfo?.let {
            val url = getRemoteMapIconUrlForModeInfo(DeviceInfo.getDensityDpiName(), it)
            picasso.load(url).into(MarkerOptionsTarget(WeakReference(markerOptions)))
        }
    }

    fun callAsync(markerOptions: MarkerOptions, stop: ScheduledStop): Single<MarkerOptions> {
        val modeInfo = stop.modeInfo
        val iconUrl =
            if(modeInfo?.remoteIconIsTemplate == true) {
                getRemoteMapIconUrlForModeInfo(DeviceInfo.getDensityDpiName(), modeInfo)
            } else {
                getLocalMapIconUrlForModeInfo(DeviceInfo.getDensityDpiName(), modeInfo)
            }
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

                                val icon = BitmapDescriptorFactory.fromBitmap(circularBitmap)
                                markerOptions.icon(icon)
                                emitter.onSuccess(markerOptions)
                            } ?: run {
                                emitter.onError(Throwable("Bitmap is null"))
                            }
                        }

                        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                            if(BuildConfig.DEBUG) {
                                e?.printStackTrace()
                            }
                            emitter.onError(e ?: Throwable("Bitmap failed to load"))
                        }

                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                            // Placeholder if needed
                        }
                    })
            }.onErrorResumeNext {
                // Fallback to local resource-based marker icon
                getMapIconFromResource(markerOptions, stop.type)
            }
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun getMapIconFromResource(markerOptions: MarkerOptions, type: StopType?): Single<MarkerOptions> {
        return Single.fromCallable {
            val iconRes = BindingConversions.convertStopTypeToMapIconRes(type)
            val icon: BitmapDescriptor = if (iconRes == 0) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            } else {
                BitmapDescriptorFactory.fromResource(iconRes)
            }
            markerOptions.icon(icon)
            markerOptions
        }
    }

    private fun createCircularMarkerBitmap(
        bitmap: Bitmap,
        tintColor: Int,
        circleColor: Int,
        circleRadius: Int
    ): Bitmap {
        // Create a new bitmap for the output
        val output = Bitmap.createBitmap(circleRadius * 2, circleRadius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw the white border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = circleRadius * 0.1f // Border thickness is 10% of the radius
        }
        canvas.drawCircle(circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat() - (borderPaint.strokeWidth / 2), borderPaint)

        // Draw the circular background inside the border
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = circleColor
        }
        canvas.drawCircle(circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat() - borderPaint.strokeWidth, backgroundPaint)

        // Apply tint to the bitmap
        val tintedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val bitmapCanvas = Canvas(tintedBitmap)
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        }
        bitmapCanvas.drawBitmap(tintedBitmap, 0f, 0f, tintPaint)

        // Scale the bitmap to fit inside the circle while maintaining the aspect ratio
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (aspectRatio > 1) {
            // Landscape orientation: Width is greater than height
            targetWidth = circleRadius * 2
            targetHeight = (targetWidth / aspectRatio).toInt()
        } else if(aspectRatio == 1f) {
            targetHeight = circleRadius
            targetWidth = circleRadius
        } else {
            // Portrait orientation: Height is greater than or equal to width
            targetHeight = circleRadius * 2
            targetWidth = (targetHeight * aspectRatio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(
            tintedBitmap,
            targetWidth,
            targetHeight,
            true
        )

        // Draw the scaled bitmap at the center of the circular background
        val left = (output.width - scaledBitmap.width) / 2f
        val top = (output.height - scaledBitmap.height) / 2f
        canvas.drawBitmap(scaledBitmap, left, top, null)

        return output
    }
}