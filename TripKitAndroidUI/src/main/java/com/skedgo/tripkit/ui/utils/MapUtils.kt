package com.skedgo.tripkit.ui.utils

import android.animation.TypeEvaluator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import android.animation.ValueAnimator

object MapUtils {

    /**
     * Converts a drawable resource to a bitmap with specified width, height, and color.
     *
     * @param context The application context.
     * @param drawableRes The drawable resource ID.
     * @param width The width of the bitmap.
     * @param height The height of the bitmap.
     * @param color The color to apply as a tint.
     * @return The generated bitmap.
     */
    fun getBitmapFromDrawable(context: Context, drawableRes: Int, width: Int, height: Int, color: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
            ?: return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Apply color filter
        drawable.setTint(color)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * Animates a marker from its current position to a target position.
     *
     * @param marker The marker to animate.
     * @param toPosition The target position.
     * @param duration The duration of the animation in milliseconds.
     */
    fun animateMarkerToPosition(marker: Marker, toPosition: LatLng, duration: Long = 1000L) {
        val startLatLng = marker.position
        val latLngEvaluator = TypeEvaluator<LatLng> { fraction, startValue, endValue ->
            LatLng(
                startValue.latitude + fraction * (endValue.latitude - startValue.latitude),
                startValue.longitude + fraction * (endValue.longitude - startValue.longitude)
            )
        }

        val animator = ValueAnimator.ofObject(latLngEvaluator, startLatLng, toPosition)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as LatLng
            marker.position = animatedValue
        }
        animator.start()
    }

    /**
     * Animates a GroundOverlay to create a pulsing effect by changing its dimensions.
     *
     * @param overlay The overlay to animate.
     * @param minSize The minimum size of the overlay in meters.
     * @param maxSize The maximum size of the overlay in meters.
     * @param duration The duration of the pulse animation in milliseconds.
     */
    fun animatePulseOverlay(overlay: GroundOverlay?, minSize: Float = 100f, maxSize: Float = 300f, duration: Long = 2500L) {
        overlay?.let { groundOverlay ->
            val animator = ValueAnimator.ofFloat(minSize, maxSize)
            animator.duration = duration
            animator.repeatCount = ValueAnimator.INFINITE
            animator.repeatMode = ValueAnimator.RESTART // Ensures it restarts instead of reversing
            animator.addUpdateListener { animation ->
                val animatedSize = animation.animatedValue as Float
                groundOverlay.setDimensions(animatedSize) // Dynamically update size
            }
            animator.start()
        }
    }
}