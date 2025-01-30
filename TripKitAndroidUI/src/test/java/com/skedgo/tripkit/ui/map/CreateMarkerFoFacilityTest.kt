package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.data.database.locations.facility.FacilityLocationEntity
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CreateMarkerFoFacilityTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var resources: Resources

    @MockK
    private lateinit var facilityLocation: FacilityLocationEntity

    @Before
    fun setUp() {
        initRx()
        MockKAnnotations.init(this, relaxed = true)

        // Mock Android resources
        every { resources.getDimensionPixelSize(R.dimen.map_icon_size) } returns 48
        every { resources.getDimensionPixelSize(R.dimen.map_icon_stroke) } returns 2
        every { resources.getDimensionPixelSize(R.dimen.map_icon_padding) } returns 4

        // Mock facility location properties
        every { facilityLocation.name } returns "Test Facility"
        every { facilityLocation.lat } returns 37.7749
        every { facilityLocation.lng } returns -122.4194

        // Mock MarkerIconManager
        mockkObject(MarkerIconManager)
        every { MarkerIconManager.getMarkerBitmap(R.drawable.ic_facility, any()) } returns mockk()
    }

    @Test
    fun `execute creates a MarkerOptions with correct properties`() {
        // Act
        val testObserver = CreateMarkerFoFacility.execute(resources, facilityLocation).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertValue { markerOptions ->
            markerOptions.title == "Test Facility" &&
                markerOptions.position == LatLng(37.7749, -122.4194) && !markerOptions.isDraggable
        }

        // Verify interactions
        verify { resources.getDimensionPixelSize(R.dimen.map_icon_size) }
        verify { resources.getDimensionPixelSize(R.dimen.map_icon_stroke) }
        verify { resources.getDimensionPixelSize(R.dimen.map_icon_padding) }
        verify { MarkerIconManager.getMarkerBitmap(R.drawable.ic_facility, 48) }
    }
}