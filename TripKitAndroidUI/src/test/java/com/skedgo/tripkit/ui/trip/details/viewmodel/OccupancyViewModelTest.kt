package com.skedgo.tripkit.ui.trip.details.viewmodel

import android.content.Context
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.routing.Occupancy
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.VehicleComponent
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OccupancyViewModelTest {

    private lateinit var viewModel: OccupancyViewModel
    private val context: Context = mockk()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { context.getString(any()) } returns "Moderate Occupancy"
        every { ContextCompat.getDrawable(context, any()) } returns mockk()
        every { ContextCompat.getColor(context, any()) } returns 0xFF0000

        viewModel = OccupancyViewModel(context)
    }

    @Test
    fun `setOccupancy updates fields correctly for single vehicle`() {
        val vehicle = mockk<RealTimeVehicle>()
        val component = mockk<VehicleComponent>() {
            every { this@mockk.occupancy() } returns Occupancy.StandingRoomOnly.value
        }

        every { vehicle.hasVehiclesOccupancy() } returns false
        every { vehicle.hasSingleVehicleOccupancy() } returns true
        every { vehicle.getAverageOccupancy() } returns Occupancy.StandingRoomOnly
        every { vehicle.components } returns listOf(listOf(component)) // Ensure it's not empty

        viewModel.setOccupancy(vehicle, showAverage = true)

        assertTrue(viewModel.hasOccupancySingleInformation.get())
        assertEquals("Moderate Occupancy", viewModel.occupancyText.get())
        assertNotNull(viewModel.drawableLeft.get())
    }

    @Test
    fun `setOccupancy updates train occupancy items when vehicle has components`() {
        val vehicle = mockk<RealTimeVehicle>()
        val component1 = mockk<VehicleComponent>(relaxed = true) {
            every { occupancy() } returns "FEW_SEATS_AVAILABLE"
        }
        val component2 = mockk<VehicleComponent>(relaxed = true) {
            every { occupancy() } returns "FULL"
        }

        every { vehicle.hasVehiclesOccupancy() } returns true
        every { vehicle.components } returns listOf(listOf(component1, component2))

        every { ContextCompat.getColor(context, any()) } returns 0xFF0000
        every { ContextCompat.getDrawable(context, any()) } returns mockk()

        viewModel.setOccupancy(vehicle, showAverage = false)

        assertTrue(viewModel.hasOccupancyInformation.get())
        assertEquals(2, viewModel.items.size) // Expecting 2 items
    }


    @Test
    fun `setOccupancy does not update train items if no components exist`() {
        val vehicle = mockk<RealTimeVehicle>()
        every { vehicle.hasVehiclesOccupancy() } returns false
        every { vehicle.components } returns emptyList()

        viewModel.setOccupancy(vehicle, showAverage = false)

        assertFalse(viewModel.hasOccupancyInformation.get())
        assertEquals(0, viewModel.items.size)
    }
}
