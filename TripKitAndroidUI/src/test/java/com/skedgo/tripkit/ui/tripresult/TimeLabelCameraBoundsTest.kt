package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.tan

/**
 * Redmine #19351: trip time pills are drawn beside their markers, so a uniform camera padding
 * clipped them at the screen edge at large font scales. The trip bounds must be widened so the
 * fitted camera leaves at least the label width between the outermost markers and the edge.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TimeLabelCameraBoundsTest {

    private val mapWidth = 1080
    private val mapHeight = 2400
    private val padding = CameraUpdatePadding

    // Town Hall -> Bondi Junction (mostly east-west).
    private val eastWestTrip = LatLngBounds(LatLng(-33.892, 151.206), LatLng(-33.873, 151.250))

    // A mostly north-south trip (height-limited fit).
    private val northSouthTrip = LatLngBounds(LatLng(-33.95, 151.205), LatLng(-33.80, 151.210))

    private fun mercatorY(lat: Double) = ln(tan(PI / 4 + Math.toRadians(lat) / 2)) / (2 * PI)

    /** Free pixels between the original route's east/west edge and the map edge after fitting. */
    private fun sideMarginPx(original: LatLngBounds, fitted: LatLngBounds): Double {
        val fittedDx = (fitted.northeast.longitude - fitted.southwest.longitude) / 360.0
        val dy = mercatorY(fitted.northeast.latitude) - mercatorY(fitted.southwest.latitude)
        val scale = min((mapWidth - 2 * padding) / fittedDx, (mapHeight - 2 * padding) / dy)
        val routeDx = (original.northeast.longitude - original.southwest.longitude) / 360.0
        return (mapWidth - routeDx * scale) / 2
    }

    @Test
    fun `east-west trip leaves room for wide time labels`() {
        val labelExtent = 360 // ~ pin + "12:58 PM" pill at font scale 2.0

        val fitted = eastWestTrip.expandedForHorizontalLabels(labelExtent, mapWidth, mapHeight, padding)

        assertTrue(sideMarginPx(eastWestTrip, fitted) >= labelExtent - 1)
        assertEquals(eastWestTrip.southwest.latitude, fitted.southwest.latitude, 0.0)
        assertEquals(eastWestTrip.northeast.latitude, fitted.northeast.latitude, 0.0)
    }

    @Test
    fun `north-south trip leaves room for time labels`() {
        val labelExtent = 300

        val fitted = northSouthTrip.expandedForHorizontalLabels(labelExtent, mapWidth, mapHeight, padding)

        assertTrue(sideMarginPx(northSouthTrip, fitted) >= labelExtent - 1)
    }

    @Test
    fun `label room is capped so the route keeps a quarter of the width`() {
        val fitted = eastWestTrip.expandedForHorizontalLabels(5_000, mapWidth, mapHeight, padding)

        val expectedRoom = mapWidth * 3.0 / 8
        assertTrue(sideMarginPx(eastWestTrip, fitted) >= expectedRoom - 1)
        assertTrue(sideMarginPx(eastWestTrip, fitted) < mapWidth / 2.0)
    }

    @Test
    fun `labels that fit in the padding keep the original bounds`() {
        assertSame(
            eastWestTrip,
            eastWestTrip.expandedForHorizontalLabels(padding, mapWidth, mapHeight, padding)
        )
    }

    @Test
    fun `unknown map size keeps the original bounds`() {
        assertSame(eastWestTrip, eastWestTrip.expandedForHorizontalLabels(360, 0, 0, padding))
    }
}
