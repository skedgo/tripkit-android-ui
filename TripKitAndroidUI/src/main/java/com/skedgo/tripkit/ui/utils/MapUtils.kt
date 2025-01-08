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
import android.annotation.SuppressLint
import androidx.core.animation.addListener
import com.skedgo.tripkit.routing.RealTimeVehicle
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

    /**
     * Updates the marker's opacity based on the fade level.
     *
     * @param marker The marker to update.
     * @param fadeLevel The calculated fade level (0.3 to 1.0).
     */
    fun updateMarkerOpacity(marker: Marker?, fadeLevel: Float) {
        marker?.alpha = fadeLevel.coerceIn(0.3f, 1.0f) // Adjusts alpha based on fade level
    }

    /**
     * Updates the overlay's transparency based on the fade level.
     *
     * @param overlay The overlay to update.
     * @param fadeLevel The calculated fade level (0.3 to 1.0).
     */
    fun updateOverlayTransparency(overlay: GroundOverlay?, fadeLevel: Float) {
        overlay?.transparency = (1f - fadeLevel).coerceIn(0.0f, 0.7f) // Adjusts transparency
    }

    /**
     * Formats elapsed time into a human-readable string (e.g., "15 seconds ago" or "1 minute and 15 seconds ago").
     *
     * @param ageInSeconds The age of the data in seconds.
     * @param vehicle The RealTimeVehicle object to include its label in the message.
     * @return The formatted elapsed time string.
     */
    @SuppressLint("DefaultLocale")
    fun formatElapsedTime(ageInSeconds: Long, vehicle: RealTimeVehicle): String {
        return if (ageInSeconds < 60) {
            "Vehicle ${vehicle.label} updated ${formatTimeUnit(ageInSeconds, "second")} ago"
        } else {
            val minutes = ageInSeconds / 60
            val seconds = ageInSeconds % 60

            if (seconds == 0L) {
                "Vehicle ${vehicle.label} updated ${formatTimeUnit(minutes, "minute")} ago"
            } else {
                "Vehicle ${vehicle.label} updated ${formatTimeUnit(minutes, "minute")} and ${formatTimeUnit(seconds, "second")} ago"
            }
        }
    }

    /**
     * Formats a time unit with proper pluralization.
     *
     * @param value The value of the time unit (e.g., 1, 15).
     * @param unit The time unit (e.g., "second", "minute").
     * @return A formatted string with singular or plural unit (e.g., "1 second", "15 seconds").
     */
    private fun formatTimeUnit(value: Long, unit: String): String {
        return "$value $unit${if (value != 1L) "s" else ""}"
    }

    /**
     * Calculates the age factor based on the elapsed time and a maximum duration.
     *
     * @param ageInSeconds The elapsed time in seconds.
     * @param maxDuration The maximum duration, in seconds, after which the factor becomes 0.
     * @return The calculated age factor, clamped between 0.0 and 1.0.
     */
    fun calculateAgeFactor(ageInSeconds: Long, maxDuration: Int = 15): Float {
        return if (ageInSeconds >= maxDuration) {
            0.0f // Fully aged at or beyond maxDuration
        } else {
            1 - (ageInSeconds / maxDuration.toFloat()) // Linearly decreases from 1 to 0
        }
    }

    /**
     * Calculates the fade level based on the age factor.
     *
     * @param ageFactor The age factor, clamped between 0.0 and 1.0.
     * @param maxFade The maximum fade level (e.g., 0.3F for 30% visibility).
     * @return The fade level, clamped between maxFade and 1.0.
     */
    fun calculateFadeFromAgeFactor(ageFactor: Float, maxFade: Float = 0.3f): Float {
        return (maxFade + (1 - maxFade) * ageFactor).coerceIn(maxFade, 1.0f)
    }

}