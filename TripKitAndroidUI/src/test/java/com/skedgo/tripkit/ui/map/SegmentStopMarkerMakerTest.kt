package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.skedgo.tripkit.ui.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SegmentStopMarkerMakerTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var segmentStopMarkerMaker: SegmentStopMarkerMaker

    @Before
    fun setUp() {
        context = mockk()
        resources = mockk()
        every { context.resources } returns resources
        segmentStopMarkerMaker = SegmentStopMarkerMaker(context)

        // Mock BitmapDescriptorFactory
        mockkStatic(BitmapDescriptorFactory::class)
        every { BitmapDescriptorFactory.fromBitmap(any()) } returns mockk<BitmapDescriptor>()

        // Mock MapMarkerUtils static method
        mockkObject(MapMarkerUtils)
    }

    @Test
    fun `test make function`() {
        val stopMarkerViewModel = mockk<StopMarkerViewModel>(relaxed = true)
        val iconSize = 100
        val bitmap = mockk<Bitmap>(relaxed = true)
        val iconSlot = slot<Bitmap>()

        // Mock dimensions and icon creation
        every { resources.getDimensionPixelSize(R.dimen.stop_circle_pin_diameter) } returns iconSize
        every {
            MapMarkerUtils.createStopMarkerIcon(
                iconSize,
                stopMarkerViewModel.strokeColor,
                stopMarkerViewModel.fillColor,
                !stopMarkerViewModel.isTravelled
            )
        } returns bitmap

        // Mock BitmapDescriptorFactory usage
        every { BitmapDescriptorFactory.fromBitmap(capture(iconSlot)) } returns mockk()

        // Act
        val markerOptions = segmentStopMarkerMaker.make(stopMarkerViewModel)

        // Assert
        verify {
            MapMarkerUtils.createStopMarkerIcon(
                iconSize,
                stopMarkerViewModel.strokeColor,
                stopMarkerViewModel.fillColor,
                !stopMarkerViewModel.isTravelled
            )
        }
        assertEquals(stopMarkerViewModel.title, markerOptions.title)
        assertEquals(stopMarkerViewModel.snippet, markerOptions.snippet)
        assertEquals(stopMarkerViewModel.position, markerOptions.position)
        assertEquals(false, markerOptions.isDraggable)
        assertEquals(stopMarkerViewModel.alpha, markerOptions.alpha, 0.0f)
        assertEquals(bitmap, iconSlot.captured)
        assertEquals(0.5f, markerOptions.anchorU, 0.0f)
        assertEquals(0.5f, markerOptions.anchorV, 0.0f)
        assertEquals(0.5f, markerOptions.infoWindowAnchorU, 0.0f)
        assertEquals(0.0f, markerOptions.infoWindowAnchorV, 0.0f)
    }
}