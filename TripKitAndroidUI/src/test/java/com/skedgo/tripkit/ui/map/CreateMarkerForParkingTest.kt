package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.parkingspots.models.Parking
import com.skedgo.tripkit.ui.R
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CreateMarkerForParkingTest {

    @MockK
    private lateinit var resources: Resources

    @MockK
    private lateinit var parking: Parking

    @MockK
    private lateinit var parkingLocation: GeoPoint

    @MockK
    private lateinit var mockBitmapDescriptor: BitmapDescriptor

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock Parking properties
        every { parking.name } returns "Test Parking"
        every { parking.location } returns parkingLocation
        every { parkingLocation.latitude } returns 40.7128
        every { parkingLocation.longitude } returns -74.0060

        // Mock BitmapDescriptorFactory to prevent crashes
        mockkStatic(BitmapDescriptorFactory::class)
        every { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW) } returns mockBitmapDescriptor
        every { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_stop_parking) } returns mockBitmapDescriptor
    }

    @Test
    fun `execute creates a MarkerOptions with correct properties`() {
        // Act
        val testObserver = CreateMarkerForParking.execute(resources, parking).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertValue { markerOptions ->
            markerOptions.title == "Test Parking" &&
                markerOptions.position == LatLng(40.7128, -74.0060) && !markerOptions.isDraggable && markerOptions.icon == mockBitmapDescriptor
        }

        // Verify interactions
        verify { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_stop_parking) }
    }

    @Test
    fun `execute uses default marker when iconRes is zero`() {

        val parking = mockk<Parking> {
            every { name } returns "Test Parking"
            every { location.latitude } returns 12.34
            every { location.longitude } returns 56.78
        }

        // Act
        val testObserver = CreateMarkerForParking.execute(resources, parking, 0).test()

        // Assert
        testObserver.assertComplete()

        // Verify `defaultMarker` was called (since `iconRes == 0`)
        verify(exactly = 1) { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW) }
        // Ensure `fromResource` was **not** called
        verify(exactly = 0) { BitmapDescriptorFactory.fromResource(any()) }
    }
}