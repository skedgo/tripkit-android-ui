package com.skedgo.tripkit.ui.tripresult.compose

import android.graphics.Color
import com.skedgo.tripkit.ui.tripresult.RoadTagChartItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the refactored cycle breakdown against the production calculation in
 * `TripSegmentCustomRecyclerViewAdapter` + `RoadTagChartAdapter`.
 */
class RoadTagChartBuilderTest {

    private fun item(label: String, length: Int, index: Int) =
        RoadTagChartItem(label = label, length = length, color = Color.DKGRAY, index = index)

    @Test
    fun `uses the segment length verbatim as the axis maximum`() {
        val chart = buildRoadTagChart(listOf(item("Main Road", 120, 3)), segmentLength = 197)

        assertEquals(197, chart.max)
        assertEquals(98, chart.middle)
    }

    @Test
    fun `rounds to the nearest hundred only when the segment length is missing`() {
        val chart = buildRoadTagChart(listOf(item("Main Road", 197, 3)), segmentLength = null)

        assertEquals(200, chart.max)
        assertEquals(100, chart.middle)
    }

    @Test
    fun `orders rows by road safety index`() {
        val chart = buildRoadTagChart(
            listOf(
                item("Other", 12, 4),
                item("Cycle Lane", 820, 0),
                item("Main Road", 120, 3),
                item("Side Road", 1010, 2)
            ),
            segmentLength = 3147
        )

        assertEquals(
            listOf("Cycle Lane", "Side Road", "Main Road", "Other"),
            chart.items.map { it.label }
        )
    }

    @Test
    fun `merges tags sharing a label by summing their lengths`() {
        val chart = buildRoadTagChart(
            listOf(
                item("Cycle Network", 1700, 1),
                item("Cycle Track", 210, 1),
                item("Cycle Network", 1047, 1)
            ),
            segmentLength = 3147
        )

        val network = chart.items.single { it.label == "Cycle Network" }
        assertEquals(2747, network.length)
        assertEquals(2, chart.items.size)
    }

    @Test
    fun `stamps the axis maximum on every row and leaves the source items untouched`() {
        val source = listOf(item("Cycle Lane", 820, 0), item("Other", 12, 4))

        val chart = buildRoadTagChart(source, segmentLength = 3147)

        assertEquals(listOf(3147, 3147), chart.items.map { it.maxProgress })
        assertEquals(listOf(0, 0), source.map { it.maxProgress })
    }

    @Test
    fun `keeps an empty breakdown empty`() {
        val chart = buildRoadTagChart(emptyList(), segmentLength = null)

        assertEquals(0, chart.max)
        assertEquals(emptyList<RoadTagChartItem>(), chart.items)
    }
}
