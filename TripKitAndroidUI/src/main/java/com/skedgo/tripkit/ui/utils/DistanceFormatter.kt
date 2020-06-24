package com.skedgo.tripkit.ui.utils

import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.floor

/**
 * Formats a distance in meters according to the following rules:
 *
 *  * Distances over 0.1 miles return distance in miles with a max of one decimal place.
 *  * Distances under 0.1 miles return distance in feet rounded down to the nearest 10.
 *  * Distances under 10 feet return the actual value in feet in normal (list) mode.
 *  * Distances under 10 feet return "now" in real-time (navigation) mode.
 *
 */
object DistanceFormatter {
    enum class DistanceUnits {
        MILES,
        KILOMETERS
    }
    private const val METERS_IN_ONE_MILE = 1609.0
    private const val METERS_IN_ONE_FOOT = 0.3048
    private const val FEET_IN_ONE_MILE = 5280.0
    private var decimalFormat: DecimalFormat? = null
    /**
     * Format a distance for display
     *
     * @param distanceInMeters the actual distance in meters.
     * @return distance string formatted according to the rules of hte formatter.
     */
    @JvmOverloads
    fun format(distanceInMeters: Int): String {
        val locale = Locale.getDefault()
        return format(distanceInMeters, locale)
    }
    /**
     * Format distance for display using specified distance units.
     *
     * @param distanceInMeters the actual distance in meters.
     * @param units miles or kilometers.
     * @return distance string formatted according to the rules of the formatter.
     */
    fun format(distanceInMeters: Int, units: DistanceUnits): String {
        return format(distanceInMeters,  Locale.getDefault(), units)
    }

    /**
     * Format distance for display using specified locale.
     *
     * @param distanceInMeters the actual distance in meters.
     * @param locale Locale that defines the number format for displaying distance.
     * @return distance string formatted according to the rules of the formatter.
     */
    fun format(distanceInMeters: Int, locale: Locale): String {
        return if (useMiles(locale)) {
            format(distanceInMeters, locale, DistanceUnits.MILES)
        } else {
            format(distanceInMeters, locale, DistanceUnits.KILOMETERS)
        }
    }

    /**
     * Format distance for display using specified locale and units.
     *
     * @param distanceInMeters the actual distance in meters.
     * @param locale Locale that defines the number format for displaying distance.
     * @param units miles or kilometers.
     * @return distance string formatted according to the rules of the formatter.
     */
    fun format(distanceInMeters: Int, locale: Locale,
               units: DistanceUnits): String {
        decimalFormat = NumberFormat.getNumberInstance(locale) as DecimalFormat
        decimalFormat!!.applyPattern("#.#")
        return if (distanceInMeters == 0) {
            ""
        } else when (units) {
            DistanceUnits.MILES -> formatMiles(distanceInMeters)
            DistanceUnits.KILOMETERS -> formatKilometers(distanceInMeters)
        }
    }

    private fun formatMiles(distanceInMeters: Int): String {
        val distanceInFeet = distanceInMeters / METERS_IN_ONE_FOOT
        return if (distanceInFeet < 10) {
            formatDistanceInFeet(distanceInFeet)
        } else {
            formatDistanceInMiles(distanceInMeters)
        }
    }

    private fun formatKilometers(distanceInMeters: Int): String {
        return if (distanceInMeters < 1000) {
            formatDistanceInMeters(distanceInMeters)
        } else  {
            formatDistanceInKilometers(distanceInMeters)
        }
    }

    private fun useMiles(locale: Locale): Boolean {
        return locale == Locale.US || locale == Locale.UK
    }

    private fun formatDistanceInMeters(distanceInMeters: Int): String {
        return String.format(Locale.getDefault(), "%s m", distanceInMeters)
    }

    private fun formatDistanceInKilometers(distanceInMeters: Int): String {
        val value = decimalFormat!!.format(distanceInMeters.toFloat() / 1000.toDouble())
        return String.format(Locale.getDefault(), "%s km", value)
    }

    private fun formatDistanceInFeet(distanceInFeet: Double): String {
        val roundedDistanceInFeet = roundDownToNearestTen(distanceInFeet)
        return String.format(Locale.getDefault(), "%d ft", roundedDistanceInFeet)
    }

    private fun formatDistanceInMiles(distanceInMeters: Int): String {
        return String.format(Locale.getDefault(), "%s mi",
                decimalFormat!!.format(distanceInMeters / METERS_IN_ONE_MILE))
    }

    private fun roundDownToNearestTen(distance: Double): Int {
        return floor(distance / 10).toInt() * 10
    }
}