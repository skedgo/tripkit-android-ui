package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import com.skedgo.tripkit.ui.routing.getVisibleGeoPointsOnMap
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

    // Derive screen margins independently from the fitted Mercator bounds. Removing northward
    // marker room or using the full screen instead of the sheet-padded viewport breaks these.
    private fun assertMarkerFits(width: Int, height: Int, label: Int, marker: Int, statusBar: Int) {
        val fitted = eastWestTrip.expandedForTimeLabels(
            label, marker, width, height, android.graphics.Rect(0, statusBar, 0, 0)
        )
        val dx = (fitted.northeast.longitude - fitted.southwest.longitude) / 360.0
        val north = mercatorY(fitted.northeast.latitude)
        val south = mercatorY(fitted.southwest.latitude)
        val scale = min((width - 2 * padding) / dx, (height - 2 * padding) / (north - south))
        val routeNorth = height / 2.0 - (mercatorY(eastWestTrip.northeast.latitude) - (north + south) / 2) * scale
        val routeSouth = height / 2.0 - (mercatorY(eastWestTrip.southwest.latitude) - (north + south) / 2) * scale
        val routeWidth = (eastWestTrip.northeast.longitude - eastWestTrip.southwest.longitude) / 360.0 * scale
        assertTrue("marker top $routeNorth must clear ${marker + statusBar}", routeNorth >= marker + statusBar - 1)
        assertTrue("route must stay above sheet", routeSouth <= height - padding + 1)
        assertTrue("time pills must fit sideways", (width - routeWidth) / 2 >= label - 1)
    }

    @Test
    fun `landscape sheet viewport clears largest-font marker and status bar`() {
        assertMarkerFits(2400, 540, 360, 190, 63)
    }

    @Test
    fun `landscape sheet viewport clears normal-font marker and status bar`() {
        assertMarkerFits(2400, 540, 240, 170, 63)
    }

    @Test
    fun `portrait framing keeps labels visible without consuming the route viewport`() {
        assertMarkerFits(1080, 1200, 360, 190, 63)
    }

    private fun assertMeasuredMarkerFits() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val text = android.view.LayoutInflater.from(context)
            .inflate(com.skedgo.tripkit.ui.R.layout.view_time_label, null) as android.widget.TextView
        val icon = com.skedgo.tripkit.ui.map.BearingMarkerIconBuilder(context,
            com.skedgo.tripkit.ui.map.TimeLabelMaker(text))
            .baseIcon(com.skedgo.tripkit.ui.R.drawable.ic_map_pin_base)
            .pointerIcon(com.skedgo.tripkit.ui.R.drawable.ic_map_pin_pointer)
            .vehicleIcon(androidx.core.content.ContextCompat.getDrawable(context,
                com.skedgo.tripkit.ui.R.drawable.ic_map_pin_pointer))
            .hasTime(true).time(1_700_000_000_000L, "UTC").build()
        val label = kotlin.math.ceil(maxOf(icon.first.width * icon.second,
            icon.first.width * (1 - icon.second)).toDouble()).toInt()
        assertMarkerFits(2400, 540, label, icon.first.height, 63)
        icon.first.recycle()
    }

    @Test
    @Config(qualifiers = "xxhdpi", fontScale = 2.0f)
    fun `measured largest-font bitmap fits above the sheet and below status bar`() {
        assertMeasuredMarkerFits()
    }

    @Test
    @Config(qualifiers = "xxhdpi", fontScale = 1.0f)
    fun `measured normal-font bitmap fits above the sheet and below status bar`() {
        assertMeasuredMarkerFits()
    }

    @Test
    fun `bottom sheet viewport ends symmetrically below the padded camera target`() {
        val actual = android.graphics.Rect(0, 0, 2400, 1080).bottomPaddedViewport(270)
        assertEquals(android.graphics.Rect(0, 0, 2400, 540), actual)
    }

    @Test
    fun `portrait camera does not shift when existing vertical clearance fits the marker`() {
        val fitted = eastWestTrip.expandedForTimeLabels(360, 190, 1080, 1200,
            android.graphics.Rect(0, 63, 0, 0))
        val originalCenterY = (mercatorY(eastWestTrip.northeast.latitude) + mercatorY(eastWestTrip.southwest.latitude)) / 2
        val fittedCenterY = (mercatorY(fitted.northeast.latitude) + mercatorY(fitted.southwest.latitude)) / 2
        assertEquals(originalCenterY, fittedCenterY, 1e-12)
    }

    private fun visibleSegment(): com.skedgo.tripkit.routing.TripSegment {
        val segment = io.mockk.mockk<com.skedgo.tripkit.routing.TripSegment>(relaxed = true)
        io.mockk.every { segment.isVisibleInContext(any()) } returns true
        io.mockk.every { segment.from } returns null
        io.mockk.every { segment.to } returns null
        io.mockk.every { segment.singleLocation } returns null
        io.mockk.every { segment.realTimeVehicle } returns null
        io.mockk.every { segment.shapes } returns arrayListOf()
        io.mockk.every { segment.streets } returns arrayListOf()
        return segment
    }

    @Test
    fun `camera includes travelled shape bends but not non-travelled service extensions`() {
        val bend = LatLng(44.29, -89.02)
        val route = listOf(LatLng(44.35, -89.08), bend, LatLng(44.42, -88.74))
        val segment = visibleSegment()
        val travelled = io.mockk.mockk<com.skedgo.tripkit.routing.Shape>()
        io.mockk.every { travelled.isTravelled } returns true
        io.mockk.every { travelled.encodedWaypoints } returns com.google.maps.android.PolyUtil.encode(route)
        val extension = io.mockk.mockk<com.skedgo.tripkit.routing.Shape>()
        io.mockk.every { extension.isTravelled } returns false
        io.mockk.every { extension.encodedWaypoints } returns com.google.maps.android.PolyUtil.encode(listOf(LatLng(45.0, -90.0)))
        io.mockk.every { segment.shapes } returns arrayListOf(travelled, extension)
        val points = listOf(segment).getVisibleGeoPointsOnMap()
        assertTrue("the southward bend must participate in the camera bounds", points.any { kotlin.math.abs(it.latitude - bend.latitude) < 1e-5 })
        assertTrue("unused service extensions must not cause excessive zoom-out", points.none { it.latitude > 44.9 })
    }

    @Test
    fun `camera includes street geometry between segment endpoints`() {
        val segment = visibleSegment()
        val street = io.mockk.mockk<com.skedgo.tripkit.common.model.Street>()
        io.mockk.every { street.encodedWaypoints() } returns com.google.maps.android.PolyUtil.encode(listOf(LatLng(44.35, -89.08), LatLng(44.29, -89.02), LatLng(44.42, -88.74)))
        io.mockk.every { segment.streets } returns arrayListOf(street)
        assertTrue(listOf(segment).getVisibleGeoPointsOnMap().any { kotlin.math.abs(it.latitude - 44.29) < 1e-5 })
    }

    @Test
    fun `landscape fit keeps label envelopes clear of measured profile and location buttons`() {
        val viewport = android.graphics.Rect(0, 0, 2400, 540)
        val profile = android.graphics.Rect(2220, 100, 2360, 240)
        val location = android.graphics.Rect(2220, 340, 2360, 480)
        val route = LatLngBounds(LatLng(44.35, -89.08), LatLng(44.37, -88.74))
        val insets = route.insetsAvoidingOverlays(viewport, android.graphics.Rect(0, 63, 0, 0), listOf(profile, location), 360, 190)!!
        val fitted = route.expandedForTimeLabels(360, 190, 2400, 540, insets)
        val north = mercatorY(fitted.northeast.latitude)
        val south = mercatorY(fitted.southwest.latitude)
        val scale = min(2160 / ((fitted.northeast.longitude - fitted.southwest.longitude) / 360), 300 / (north - south))
        val x = 1200 + (route.northeast.longitude - (fitted.northeast.longitude + fitted.southwest.longitude) / 2) / 360 * scale
        val y = 270 - (mercatorY(route.northeast.latitude) - (north + south) / 2) * scale
        val marker = android.graphics.Rect((x - 360).toInt(), (y - 190).toInt(), (x + 360).toInt(), y.toInt())
        assertTrue("time label must not sit under the profile control", !android.graphics.Rect.intersects(marker, profile))
        assertTrue(!android.graphics.Rect.intersects(marker, location))
        val routePixelWidth = (route.northeast.longitude - route.southwest.longitude) / 360 * scale
        assertTrue("prefer the side strip over squeezing the route into a tiny horizontal band", routePixelWidth > 1000)
    }
}
