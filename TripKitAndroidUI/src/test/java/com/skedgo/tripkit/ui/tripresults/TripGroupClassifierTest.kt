package com.skedgo.tripkit.ui.tripresults

import com.google.gson.Gson
import com.skedgo.tripkit.routing.Availability
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.tripresults.TripGroupClassifier.Classification.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TripGroupClassifierTest {
    private fun trip(calories: Float = 0f, score: Float = 10f, seconds: Long = 1800,
                     price: Float = Trip.UNKNOWN_COST, hassle: Float = 10f,
                     carbon: Float = 10f, mode: String = "walk"): Trip = Trip().apply {
        caloriesCost = calories
        weightedScore = score
        startTimeInSecs = 1000
        endTimeInSecs = 1000 + seconds
        moneyCost = price
        moneyUsdCost = price
        hassleCost = hassle
        carbonCost = carbon
        segmentList = arrayListOf(TripSegment().apply { transportModeId = mode })
    }

    private fun group(vararg trips: Trip): TripGroup = TripGroup().apply {
        trips.forEach { addTrip(it) }
    }

    @Test fun `walking maximum calories receives active badge and non-winner does not`() {
        val walking = group(trip(calories = 150f, score = 12f))
        val bus = group(trip(calories = 20f, mode = "pt_pub_bus"))
        val classifier = TripGroupClassifier(listOf(walking, bus))
        assertThat(classifier.classify(walking)).isEqualTo(HEALTHIEST)
        assertThat(classifier.classify(bus)).isEqualTo(NONE)
    }

    @Test fun `cycling qualifies by calories rather than walking duration`() {
        val cycle = group(trip(calories = 200f, score = 12f, seconds = 1500, mode = "bicycle"))
        val walk = group(trip(calories = 100f))
        assertThat(TripGroupClassifier(listOf(cycle, walk)).classify(cycle)).isEqualTo(HEALTHIEST)
    }

    @Test fun `mixed transit and walking qualifies without mode restrictions`() {
        val mixed = group(trip(calories = 150f, score = 12f, mode = "pt_pub_bus").apply {
            segmentList.add(TripSegment().apply { transportModeId = "walk" })
        })
        val other = group(trip(calories = 10f))
        assertThat(TripGroupClassifier(listOf(mixed, other)).classify(mixed)).isEqualTo(HEALTHIEST)
    }

    @Test fun `forty kcal improvement is insufficient`() {
        val active = group(trip(calories = 60f, score = 12f))
        val other = group(trip(calories = 20f))
        assertThat(TripGroupClassifier(listOf(active, other)).classify(active)).isEqualTo(NONE)
    }

    @Test fun `more than forty kcal improvement qualifies`() {
        val active = group(trip(calories = 60.1f, score = 12f))
        val other = group(trip(calories = 20f))
        assertThat(TripGroupClassifier(listOf(active, other)).classify(active)).isEqualTo(HEALTHIEST)
    }

    @Test fun `active improvement is relative to recommended rather than worst trip`() {
        val active = group(trip(calories = 120f, score = 20f))
        val recommended = group(trip(calories = 100f, score = 10f))
        val worst = group(trip(calories = 0f, score = 30f))
        assertThat(TripGroupClassifier(listOf(active, recommended, worst)).classify(active)).isEqualTo(NONE)
    }

    @Test fun `tied active winners both qualify when recommended has fewer calories`() {
        val a = group(trip(calories = 150f, score = 20f))
        val b = group(trip(calories = 150f, score = 30f))
        val recommended = group(trip(calories = 20f, score = 10f))
        val classifier = TripGroupClassifier(listOf(a, b, recommended))
        assertThat(classifier.classify(a)).isEqualTo(HEALTHIEST)
        assertThat(classifier.classify(b)).isEqualTo(HEALTHIEST)
        assertThat(classifier.classify(recommended)).isEqualTo(RECOMMENDED)
    }

    @Test fun `equal positive calories have no active winner`() {
        val a = group(trip(calories = 100f))
        val b = group(trip(calories = 100f))
        assertThat(TripGroupClassifier(listOf(a, b)).classify(a)).isEqualTo(NONE)
    }

    @Test fun `zero calories and empty groups have no active winner`() {
        val zero = group(trip())
        val empty = group()
        val classifier = TripGroupClassifier(listOf(zero, empty))
        assertThat(classifier.classify(zero)).isEqualTo(NONE)
        assertThat(classifier.classify(empty)).isEqualTo(NONE)
        assertThat(TripGroupClassifier(emptyList()).classify(empty)).isEqualTo(NONE)
    }

    @Test fun `classification uses lowest score non-cancelled representative instead of display trip`() {
        val displayed = trip(calories = 200f, score = 30f)
        val best = trip(calories = 20f, score = 10f)
        val a = group(displayed, best)
        val b = group(trip(calories = 100f, score = 10f))
        assertThat(TripGroupClassifier(listOf(a, b)).classify(a)).isEqualTo(NONE)
    }

    @Test fun `cancelled candidate cannot win`() {
        val cancelled = group(trip(calories = 200f).apply { setAvailability(Availability.Cancelled) })
        val available = group(trip(calories = 20f))
        assertThat(TripGroupClassifier(listOf(cancelled, available)).classify(cancelled)).isEqualTo(NONE)
    }

    @Test fun `recommended takes priority over active`() {
        val recommended = group(trip(calories = 200f, score = 5f))
        val other = group(trip(calories = 20f, score = 20f))
        assertThat(TripGroupClassifier(listOf(recommended, other)).classify(recommended)).isEqualTo(RECOMMENDED)
    }

    @Test fun `fastest still takes priority over active with meaningful saving`() {
        val fast = group(trip(calories = 200f, score = 12f, seconds = 600))
        val slow = group(trip(calories = 20f, seconds = 1800))
        assertThat(TripGroupClassifier(listOf(fast, slow)).classify(fast)).isEqualTo(FASTEST)
    }

    @Test fun `small fastest saving does not preempt active badge`() {
        val active = group(trip(calories = 200f, score = 12f, seconds = 1500))
        val other = group(trip(calories = 20f, seconds = 1800))
        assertThat(TripGroupClassifier(listOf(active, other)).classify(active)).isEqualTo(HEALTHIEST)
    }

    @Test fun `easiest remains easiest with meaningful hassle improvement`() {
        val easy = group(trip(hassle = 1f, score = 12f))
        val hard = group(trip(hassle = 10f))
        assertThat(TripGroupClassifier(listOf(easy, hard)).classify(easy)).isEqualTo(EASIEST)
    }

    @Test fun `greenest remains greenest`() {
        val green = group(trip(carbon = 1f, score = 12f))
        val dirty = group(trip(carbon = 10f))
        assertThat(TripGroupClassifier(listOf(green, dirty)).classify(green)).isEqualTo(GREENEST)
    }

    @Test fun `cheapest uses USD metric and requires over five USD improvement`() {
        val cheap = group(trip(price = 100f, score = 12f).apply { moneyUsdCost = 1f })
        val dear = group(trip(price = 100f).apply { moneyUsdCost = 10f })
        assertThat(TripGroupClassifier(listOf(cheap, dear)).classify(cheap)).isEqualTo(CHEAPEST)
    }

    @Test fun `unknown price suppresses cheapest`() {
        val known = group(trip(price = 1f))
        val unknown = group(trip())
        assertThat(TripGroupClassifier(listOf(known, unknown)).classify(known)).isEqualTo(NONE)
    }

    @Test fun `stored Leicester results give walking the same active category as iOS`() {
        val groups = javaClass.getResourceAsStream("/most_active_leicester.json")!!.reader().use {
            Gson().fromJson(it, Array<TripGroup>::class.java).toList()
        }
        val classifier = TripGroupClassifier(groups)
        assertThat(groups.map { classifier.classify(it) }).containsExactly(
            NONE, RECOMMENDED, NONE, HEALTHIEST, NONE, GREENEST, NONE, NONE
        )
        val walking = groups[3].trips!!.minByOrNull { it.weightedScore }!!
        assertThat(walking.caloriesCost).isEqualTo(101f)
        assertThat(walking.durationInSeconds()).isEqualTo(2425)
    }


    @Test fun `exact twenty five percent score boundary does not receive recommended`() {
        val a = group(trip(score = 0.7f))
        val b = group(trip(score = 0.875f))
        assertThat(TripGroupClassifier(listOf(a, b)).classify(a)).isEqualTo(NONE)
    }

    @Test fun `cancelled lowest score is ignored in favor of available representative`() {
        val cancelled = trip(calories = 250f, score = 1f).apply { setAvailability(Availability.Cancelled) }
        val available = trip(calories = 150f, score = 12f)
        val active = group(cancelled, available)
        val other = group(trip(calories = 20f))
        assertThat(TripGroupClassifier(listOf(active, other)).classify(active)).isEqualTo(HEALTHIEST)
    }

    @Test fun `exact five USD saving does not receive cheapest`() {
        val cheap = group(trip(price = 1f, score = 12f))
        val dear = group(trip(price = 6f))
        assertThat(TripGroupClassifier(listOf(cheap, dear)).classify(cheap)).isEqualTo(NONE)
    }

    @Test fun `exact five hassle saving does not receive easiest`() {
        val easy = group(trip(hassle = 1f, score = 12f))
        val hard = group(trip(hassle = 6f))
        assertThat(TripGroupClassifier(listOf(easy, hard)).classify(easy)).isEqualTo(NONE)
    }

    @Test fun `exact ten timestamp minute saving does not preempt active`() {
        val active = group(trip(calories = 150f, score = 12f).apply {
            startTimeInSecs = 1019
            endTimeInSecs = 1601 // 26 - 16 = 10 timestamp minutes
        })
        val other = group(trip(calories = 20f).apply {
            startTimeInSecs = 1019
            endTimeInSecs = 2201 // 36 - 16 = 20 timestamp minutes
        })
        assertThat(TripGroupClassifier(listOf(active, other)).classify(active)).isEqualTo(HEALTHIEST)
    }

}
