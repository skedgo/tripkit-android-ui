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
import androidx.core.animation.addListener
import kotlin.math.pow

object MapUtils {

    private var pulseAnimator: ValueAnimator? = null
    private var hideAnimator: ValueAnimator? = null

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
     * @param zoomLevel The current zoom level of the map.
     * @param baseMinSize The base minimum size of the overlay in meters (at reference zoom level).
     * @param baseMaxSize The base maximum size of the overlay in meters (at reference zoom level).
     * @param duration The duration of the pulse animation in milliseconds.
     */
    fun animatePulseOverlay(
        overlay: GroundOverlay?,
        zoomLevel: Float,
        baseMinSize: Float = 150f,
        baseMaxSize: Float = 350f,
        duration: Long = 2500L
    ) {
        overlay?.let { groundOverlay ->
            // Stop any existing animation
            pulseAnimator?.cancel()

            // Adjust min and max sizes based on zoom level
            val baselineZoom = 15f
            val scaleFactor = 2.0.pow((baselineZoom - zoomLevel).toDouble()).toFloat()
            val adjustedMinSize = baseMinSize * scaleFactor // Use / instead of * for reversed scaling
            val adjustedMaxSize = baseMaxSize * scaleFactor

            // Debugging log to verify sizes
            println("Zoom Level: $zoomLevel, Min Size: $adjustedMinSize, Max Size: $adjustedMaxSize")

            pulseAnimator = ValueAnimator.ofFloat(adjustedMinSize, adjustedMaxSize).apply {
                this.duration = duration
                this.repeatCount = ValueAnimator.INFINITE
                this.repeatMode = ValueAnimator.RESTART // Ensures it restarts instead of reversing
                addUpdateListener { animation ->
                    val animatedSize = animation.animatedValue as Float
                    groundOverlay.setDimensions(animatedSize) // Dynamically update size
                }
                start()
            }
        }
    }

    /**
     * Animates hiding of a GroundOverlay by fading out and then removing it.
     *
     * @param overlay The GroundOverlay to hide and remove.
     * @param duration The duration of the fade-out animation in milliseconds.
     */
    fun hidePulseOverlay(overlay: GroundOverlay?, duration: Long = 500L) {
        overlay?.let { groundOverlay ->
            // Stop any existing animations
            pulseAnimator?.cancel()
            hideAnimator?.cancel()

            // Animate the transparency to fade out
            hideAnimator = ValueAnimator.ofFloat(0.5f, 1.0f).apply {
                this.duration = duration
                addUpdateListener { animation ->
                    val transparency = animation.animatedValue as Float
                    groundOverlay.transparency = transparency
                }
                addListener(onEnd = {
                    // Remove the overlay after the animation ends
                    groundOverlay.remove()
                })
                start()
            }
        }
    }

}