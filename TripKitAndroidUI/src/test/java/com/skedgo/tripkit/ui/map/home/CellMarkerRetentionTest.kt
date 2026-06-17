package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CellMarkerRetentionTest {

    @Test
    fun `unchanged visible cells with same hash are retained`() {
        val previous = mapOf("1#1" to 10L, "1#2" to 20L)
        val current = mapOf("1#1" to 10L, "1#2" to 20L)

        val delta = computeCellMarkerDelta(previous, current)

        assertTrue(delta.enteredCells.isEmpty())
        assertTrue(delta.exitedCells.isEmpty())
        assertTrue(delta.changedCells.isEmpty())
        assertEquals(setOf("1#1", "1#2"), delta.unchangedCells)
    }

    @Test
    fun `entered cells are detected`() {
        val previous = mapOf("1#1" to 10L)
        val current = mapOf("1#1" to 10L, "1#2" to 20L)

        val delta = computeCellMarkerDelta(previous, current)

        assertEquals(setOf("1#2"), delta.enteredCells)
        assertTrue(delta.exitedCells.isEmpty())
    }

    @Test
    fun `exited cells are detected`() {
        val previous = mapOf("1#1" to 10L, "1#2" to 20L)
        val current = mapOf("1#2" to 20L)

        val delta = computeCellMarkerDelta(previous, current)

        assertEquals(setOf("1#1"), delta.exitedCells)
        assertTrue(delta.enteredCells.isEmpty())
    }

    @Test
    fun `stable cell hash change is detected`() {
        val previous = mapOf("1#1" to 10L)
        val current = mapOf("1#1" to 11L)

        val delta = computeCellMarkerDelta(previous, current)

        assertEquals(setOf("1#1"), delta.changedCells)
        assertTrue(delta.unchangedCells.isEmpty())
    }

    @Test
    fun `missing hash falls back per cell only`() {
        val previous = mapOf("1#1" to 10L, "1#2" to 20L)
        val current = mapOf("1#1" to null, "1#2" to 20L)

        val delta = computeCellMarkerDelta(previous, current)

        assertEquals(setOf("1#1"), delta.changedCells)
        assertEquals(setOf("1#2"), delta.unchangedCells)
        assertEquals(1, delta.missingHashCount)
    }

    @Test
    fun `same cell hash and same bounds suppress reload`() {
        val previous = mapOf("AU_NT_Darwin" to 100L)
        val current = mapOf("AU_NT_Darwin" to 100L)
        val previousBounds = bounds(swLat = -12.80, swLon = 130.80, neLat = -12.20, neLon = 131.00)
        val currentBounds = bounds(swLat = -12.80, swLon = 130.80, neLat = -12.20, neLon = 131.00)

        val decision = shouldSuppressMarkerReload(previous, current, previousBounds, currentBounds)

        assertTrue(decision.suppressReload)
    }

    @Test
    fun `same cell hash and expanded bounds do not suppress reload`() {
        val previous = mapOf("AU_NT_Darwin" to 100L)
        val current = mapOf("AU_NT_Darwin" to 100L)
        val previousBounds = bounds(swLat = -12.50, swLon = 130.90, neLat = -12.30, neLon = 131.00)
        val currentBounds = bounds(swLat = -13.00, swLon = 130.20, neLat = -12.00, neLon = 131.40)

        val decision = shouldSuppressMarkerReload(previous, current, previousBounds, currentBounds)

        assertTrue(!decision.suppressReload)
        assertTrue(decision.boundsDelta.expanded)
    }

    @Test
    fun `same region key hash and moved region only bounds do not suppress reload`() {
        val previous = mapOf("AU_NT_Darwin" to 100L)
        val current = mapOf("AU_NT_Darwin" to 100L)
        val previousBounds = bounds(swLat = -12.90, swLon = 130.80, neLat = -12.50, neLon = 131.10)
        val currentBounds = bounds(swLat = -12.90, swLon = 130.20, neLat = -12.50, neLon = 130.50)

        val decision = shouldSuppressMarkerReload(previous, current, previousBounds, currentBounds)

        assertTrue(!decision.suppressReload)
    }

    @Test
    fun `entered exited changed cells do not suppress reload`() {
        val previous = mapOf("1#1" to 10L, "1#2" to 20L)
        val current = mapOf("1#2" to 20L, "1#3" to 30L)
        val previousBounds = bounds(swLat = -33.9, swLon = 151.1, neLat = -33.7, neLon = 151.3)
        val currentBounds = bounds(swLat = -33.9, swLon = 151.1, neLat = -33.7, neLon = 151.3)

        val decision = shouldSuppressMarkerReload(previous, current, previousBounds, currentBounds)

        assertTrue(!decision.suppressReload)
    }

    private fun bounds(swLat: Double, swLon: Double, neLat: Double, neLon: Double): LatLngBounds {
        return LatLngBounds(
            southwest = LatLng(swLat, swLon),
            northeast = LatLng(neLat, neLon)
        )
    }
}
