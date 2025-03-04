package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.locations.CarPod
import com.skedgo.tripkit.locations.ModeInfo
import com.skedgo.tripkit.locations.ModeInfoColor
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripGoStyleKit
import com.squareup.picasso.Picasso
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
class CreateMarkerForCarPodTest {

    @MockK
    private lateinit var resources: Resources

    @MockK
    private lateinit var picasso: Picasso

    @MockK
    private lateinit var carPod: CarPod

    @MockK
    private lateinit var modeInfo: ModeInfo

    @MockK
    private lateinit var colorEntity: ModeInfoColor

    @MockK
    private lateinit var mockBitmapDescriptor: BitmapDescriptor

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock resource dimensions
        every { resources.getDimensionPixelSize(R.dimen.map_icon_size) } returns 48
        every { resources.getDimensionPixelSize(R.dimen.map_icon_stroke) } returns 2
        every { resources.getDimensionPixelSize(R.dimen.map_icon_padding) } returns 4

        // Mock carPod properties
        every { carPod.name } returns "Test CarPod"
        every { carPod.lat } returns 37.7749
        every { carPod.lng } returns -122.4194
        every { carPod.address } returns "123 CarPod Street"
        every { carPod.modeInfo } returns modeInfo

        // Mock ModeInfo & ColorEntity
        every { modeInfo.color } returns colorEntity
        every { colorEntity.red } returns 255
        every { colorEntity.green } returns 100
        every { colorEntity.blue } returns 50

        // Mock BitmapDescriptorFactory to prevent crashes
        mockkStatic(BitmapDescriptorFactory::class)
        every { BitmapDescriptorFactory.fromBitmap(any()) } returns mockBitmapDescriptor
    }

    @Test
    fun `execute creates a MarkerOptions with correct properties`() {
        // Act
        val testObserver = CreateMarkerForCarPod.execute(resources, picasso, carPod).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertValue { markerOptions ->
            markerOptions.title == "Test CarPod" &&
                markerOptions.position == LatLng(37.7749, -122.4194) &&
                markerOptions.snippet == "123 CarPod Street" && !markerOptions.isDraggable && markerOptions.icon == mockBitmapDescriptor
        }

        // Verify interactions
        verify { resources.getDimensionPixelSize(R.dimen.map_icon_size) }
        verify { BitmapDescriptorFactory.fromBitmap(any()) }
    }

    @Test
    fun `execute handles null modeInfo or colorEntity`() {
        // Arrange: Set modeInfo & colorEntity to null
        every { carPod.modeInfo } returns null

        // Act
        val testObserver = CreateMarkerForCarPod.execute(resources, picasso, carPod).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }
}