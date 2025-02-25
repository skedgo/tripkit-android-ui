package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.data.database.locations.bikepods.ModeInfoEntity
import com.skedgo.tripkit.data.database.locations.bikepods.ServiceColorEntity
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingOperatorEntity
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingVehicleEntity
import com.skedgo.tripkit.locations.ModeInfo
import com.skedgo.tripkit.locations.ModeInfoColor
import com.skedgo.tripkit.locations.Operator
import com.skedgo.tripkit.locations.Vehicle
import com.skedgo.tripkit.ui.R
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CreateMarkerForFreeFloatingLocationTest {

    @MockK
    private lateinit var resources: Resources

    @MockK
    private lateinit var freeFloatingLocation: FreeFloatingLocationEntity

    @MockK
    private lateinit var modeInfo: ModeInfoEntity

    @MockK
    private lateinit var colorEntity: ServiceColorEntity

    @MockK
    private lateinit var vehicle: FreeFloatingVehicleEntity

    @MockK
    private lateinit var operator: FreeFloatingOperatorEntity

    @MockK
    private lateinit var mockBitmapDescriptor: BitmapDescriptor

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock resource dimensions
        every { resources.getDimensionPixelSize(R.dimen.free_floating_map_icon_size) } returns 48

        // Mock FreeFloatingLocationEntity properties
        every { freeFloatingLocation.lat } returns 37.7749
        every { freeFloatingLocation.lng } returns -122.4194
        every { freeFloatingLocation.modeInfo } returns modeInfo
        every { freeFloatingLocation.vehicle } returns vehicle

        // Mock Vehicle and Operator
        every { vehicle.operator } returns operator
        every { operator.name } returns "Test Operator"

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
        val testObserver = CreateMarkerForFreeFloatingLocation.execute(resources, freeFloatingLocation).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertValue { markerOptions ->
            markerOptions.title == "Test Operator" &&
                markerOptions.position == LatLng(37.7749, -122.4194) && !markerOptions.isDraggable && markerOptions.icon == mockBitmapDescriptor
        }

        // Verify interactions
        verify { resources.getDimensionPixelSize(R.dimen.free_floating_map_icon_size) }
        verify { BitmapDescriptorFactory.fromBitmap(any()) }
    }

    @Test
    fun `execute handles null modeInfo or colorEntity`() {
        // Arrange: Set modeInfo to null
        every { freeFloatingLocation.modeInfo } returns null

        // Act
        val testObserver = CreateMarkerForFreeFloatingLocation.execute(resources, freeFloatingLocation).test()

        // Assert
        testObserver.assertComplete()
        verify { BitmapDescriptorFactory.fromBitmap(any()) }
    }
}