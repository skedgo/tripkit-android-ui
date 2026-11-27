package com.skedgo.tripkit.ui.tripresults

import com.skedgo.tripkit.routing.Availability
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import kotlin.math.max
import kotlin.math.min


class TripGroupClassifier constructor(tripGroups: List<TripGroup>) {
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
    private var carbons = floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

    var FloatArray.second
        set(value) = set(1, value)
        get() = get(1)

    var FloatArray.first
        set(value) = set(0, value)
        get() = get(0)

    init {
        var anyHaveUnknownCost = false
        // Match iOS TKMetricClassifier: classify the best non-cancelled trip in each group.
        val trips = tripGroups.mapNotNull { it.representativeTrip }
        trips.forEach { trip ->
            if (trip.moneyUsdCost == Trip.UNKNOWN_COST) {
                anyHaveUnknownCost = true
            } else {
                prices.first = min(prices.first, trip.moneyUsdCost)
                prices.second = max(prices.second, trip.moneyUsdCost)
            }

            weighted.first = min(weighted.first, trip.weightedScore)
            weighted.second = max(weighted.second, trip.weightedScore)

            durations.first = min(durations.first, trip.classificationMinutes)
            durations.second = max(durations.second, trip.classificationMinutes)

            hassles.first = min(hassles.first, trip.hassleCost)
            hassles.second = max(hassles.second, trip.hassleCost)

            carbons.first = min(carbons.first, trip.carbonCost)
            carbons.second = max(carbons.second, trip.carbonCost)

            // Inverted
            calories.first = min(calories.first, trip.caloriesCost * -1)
            calories.second = max(calories.second, trip.caloriesCost * -1)
        }

        // Other badges must improve on the recommended trip, not merely the worst result.
        trips.minByOrNull { it.weightedScore }?.let { recommended ->
            prices.second = recommended.moneyUsdCost
            durations.second = recommended.classificationMinutes
            hassles.second = recommended.hassleCost
            carbons.second = recommended.carbonCost
            calories.second = recommended.caloriesCost * -1
        }

        if (anyHaveUnknownCost) {
            prices.first = 0.0f
            prices.second = 0.0f
        }
    }

    fun classify(tripGroup: TripGroup): Classification {
        val trip = tripGroup.representativeTrip ?: return Classification.NONE
        val classification = when {
            matches(
                weighted.first,
                weighted.second,
                trip.weightedScore
            ) -> Classification.RECOMMENDED
            matches(
                durations.first,
                durations.second,
                trip.classificationMinutes,
                minimumDelta = 10f
            ) -> Classification.FASTEST
            matches(prices.first, prices.second, trip.moneyUsdCost, minimumDelta = 5f) -> Classification.CHEAPEST
            matches(
                calories.first,
                calories.second,
                trip.caloriesCost * -1,
                minimumDelta = 40f
            ) -> Classification.HEALTHIEST
            matches(hassles.first, hassles.second, trip.hassleCost, minimumDelta = 5f) -> Classification.EASIEST
            matches(carbons.first, carbons.second, trip.carbonCost) -> Classification.GREENEST
            else -> Classification.NONE
        }
        return classification
    }

    private fun matches(min: Float, max: Float, value: Float, minimumDelta: Float? = null): Boolean =
        min == value && max > min * 1.25f &&
            (minimumDelta == null || max - min > minimumDelta)

    private val TripGroup.representativeTrip: Trip?
        get() = trips?.filter { it.getAvailability() != Availability.Cancelled }
            ?.minByOrNull { it.weightedScore }

    // iOS compares integer arrival/departure minutes rather than fractional duration.
    private val Trip.classificationMinutes: Float
        get() = (endTimeInSecs / 60 - startTimeInSecs / 60).toFloat()

}