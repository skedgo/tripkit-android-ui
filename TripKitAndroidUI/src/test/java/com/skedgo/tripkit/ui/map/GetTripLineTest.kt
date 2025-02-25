package com.skedgo.tripkit.ui.map

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GetTripLineTest {

    private lateinit var serviceLineOverlayTask: ServiceLineOverlayTask

    @Before
    fun setUp() {
        serviceLineOverlayTask = ServiceLineOverlayTask()
    }

    @Test
    fun `apply should generate correct polylines for travelled and non-travelled segments`() {
        // Arrange
        val travelledWaypoints = listOf(
            LatLng(37.7749, -122.4194),
            LatLng(37.7750, -122.4195)
        )
        val nonTravelledWaypoints = listOf(
            LatLng(37.7751, -122.4196),
            LatLng(37.7752, -122.4197)
        )

        val travelledLine = ServiceLineOverlayTask.ServiceLineInfo(
            travelledWaypoints,
            Color.RED,
            true
        )

        val nonTravelledLine = ServiceLineOverlayTask.ServiceLineInfo(
            nonTravelledWaypoints,
            Color.BLUE,
            false
        )

        val serviceLineInfos = listOf(travelledLine, nonTravelledLine)

        // Act
        val polylineOptionsList = serviceLineOverlayTask.apply(serviceLineInfos)

        // Assert
        assertEquals(3, polylineOptionsList.size) // One black outline, one traveled, one non-traveled

        // Check traveled line (colored and black outline)
        assertEquals(Color.BLACK, polylineOptionsList[0].color) // Black outline
        assertEquals(Color.RED, polylineOptionsList[1].color) // Traveled segment color

        // Check non-traveled line
        assertEquals(ServiceLineOverlayTask.NON_TRAVELLED_LINE_COLOR, polylineOptionsList[2].color) // Non-traveled color
    }
}