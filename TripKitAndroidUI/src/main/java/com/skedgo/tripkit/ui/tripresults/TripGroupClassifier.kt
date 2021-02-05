package com.skedgo.tripkit.ui.tripresults

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import kotlin.math.max
import kotlin.math.min


class TripGroupClassifier constructor (tripGroups: List<TripGroup>){
    enum class Classification {
        NONE,
        CHEAPEST,
        HEALTHIEST,
        FASTEST,
        EASIEST,
        GREENEST,
        RECOMMENDED
    }

    private var weighted = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
    private var prices = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
    private var hassles = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
    private var durations = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
    private var calories = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
    private var carbons =  floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

    var FloatArray.second
        set(value) = set(1, value)
        get() = get(1)

    var FloatArray.first
        set(value) = set(0, value)
        get() = get(0)

    init {
        var anyHaveUnknownCost = false
        val trips = tripGroups.mapNotNull { it.displayTrip }
        trips.forEach { trip ->
            if (trip.moneyCost == Trip.UNKNOWN_COST) {
                anyHaveUnknownCost = true
            } else {
                prices.first = min(prices.first, trip.moneyCost)
                prices.second = max(prices.second, trip.moneyCost)
            }

            weighted.first = min(weighted.first, trip.weightedScore)
            weighted.second = max(weighted.second, trip.weightedScore)

            durations.first = min(durations.first, trip.durationInSeconds().toFloat())
            durations.second = max(durations.second, trip.durationInSeconds().toFloat())

            hassles.first = min(hassles.first, trip.hassleCost)
            hassles.second = max(hassles.second, trip.hassleCost)

            carbons.first = min(carbons.first, trip.carbonCost)
            carbons.second = max(carbons.second, trip.carbonCost)

            // Inverted
            calories.first = min(calories.first, trip.caloriesCost * -1)
            calories.second = max(calories.second, trip.caloriesCost * -1)
        }

        if (anyHaveUnknownCost) {
            prices.first = 0.0f
            prices.second = 0.0f
        }
    }

    fun classify(tripGroup: TripGroup): Classification {
        val trip = tripGroup.displayTrip ?: return Classification.NONE
        return when {
            matches(weighted.first, weighted.second, trip.weightedScore) -> Classification.RECOMMENDED
            matches(durations.first, durations.second, trip.durationInSeconds().toFloat()) -> Classification.FASTEST
            matches(prices.first, prices.second, trip.moneyCost) -> Classification.CHEAPEST
            matches(calories.first, calories.second, trip.caloriesCost * -1) -> Classification.HEALTHIEST
            matches(hassles.first, hassles.second, trip.hassleCost) -> Classification.EASIEST
            matches(carbons.first, carbons.second, trip.carbonCost) -> Classification.GREENEST
            else -> Classification.NONE
        }
    }

    private fun matches(min: Float, max: Float, value: Float): Boolean = (min == value && max > (min * 1.25))

}