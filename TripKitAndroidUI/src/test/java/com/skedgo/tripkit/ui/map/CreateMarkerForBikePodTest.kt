package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodEntity
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodLocationEntity
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripGoStyleKit
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CreateMarkerForBikePodTest {

    @MockK
    private lateinit var resources: Resources

    @MockK
    private lateinit var bikePod: BikePodLocationEntity

    @MockK
    private lateinit var bikePodDetails: BikePodEntity

    @MockK
    private lateinit var mockBitmapDescriptor: BitmapDescriptor

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock resource dimensions
        every { resources.getDimensionPixelSize(R.dimen.map_icon_size) } returns 48

        // Mock bikePod details
        every { bikePod.bikePod } returns bikePodDetails
        every { bikePod.lat } returns 37.7749
        every { bikePod.lng } returns -122.4194
        every { bikePodDetails.operator.name } returns "Test BikePod"
        every { bikePodDetails.availableBikes } returns 5
        every { bikePodDetails.totalSpaces } returns 10

        mockkStatic(TripGoStyleKit::class)
        every { TripGoStyleKit.drawBikeShareMap(any(), any(), any(), any()) } just Runs

        mockkStatic(BitmapDescriptorFactory::class)
        every { BitmapDescriptorFactory.fromBitmap(any()) } returns mockBitmapDescriptor
    }

    @Test
    fun `execute creates a MarkerOptions with correct properties`() {
        // Act
        val testObserver = CreateMarkerForBikePod.execute(resources, bikePod).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertValue { markerOptions ->
            markerOptions.title == "Test BikePod" &&
                markerOptions.position == LatLng(
                37.7749,
                -122.4194
            ) && !markerOptions.isDraggable && markerOptions.icon == mockBitmapDescriptor
        }

        // Verify interactions
        verify { resources.getDimensionPixelSize(R.dimen.map_icon_size) }
        verify { TripGoStyleKit.drawBikeShareMap(any(), 0.5f, 0.toFloat(), 48.toFloat()) }
        verify { BitmapDescriptorFactory.fromBitmap(any()) }
    }

    @Test
    fun `execute handles null available bikes or spaces`() {
        // Arrange
        every { bikePodDetails.availableBikes } returns null
        every { bikePodDetails.totalSpaces } returns null

        // Act
        val testObserver = CreateMarkerForBikePod.execute(resources, bikePod).test()

        // Assert
        testObserver.assertComplete()
        verify { TripGoStyleKit.drawBikeShareMap(any(), 1f, 0.toFloat(), 48.toFloat()) }
    }
}