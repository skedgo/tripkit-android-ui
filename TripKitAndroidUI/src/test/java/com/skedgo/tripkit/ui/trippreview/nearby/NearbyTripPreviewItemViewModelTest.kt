package com.skedgo.tripkit.ui.trippreview.nearby

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.VehicleMode
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NearbyTripPreviewItemViewModelTest: MockKTest() {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: NearbyTripPreviewItemViewModel

    @Before
    fun setUp() {
        initRx()
        viewModel = NearbyTripPreviewItemViewModel()
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `test adding mode updates transport modes list`() {
        // Mock ModeInfo
        val mockModeInfo: ModeInfo = mockk(relaxed = true)
        val mockVehicleMode: VehicleMode = mockk(relaxed = true)

        every { mockModeInfo.id } returns "bus"
        every { mockModeInfo.modeCompat } returns mockVehicleMode

        // Execute
        viewModel.addMode(mockModeInfo)

        // Verify
        assertEquals(1, viewModel.transportModes.size)
        assertEquals("bus", viewModel.transportModes[0].modeId.get())
        assertTrue(viewModel.showModes.get())
    }


    @Test
    fun `test clearing transport modes empties the list`() {
        // Add a mode first
        val mockModeInfo: ModeInfo = mockk(relaxed = true)
        every { mockModeInfo.id } returns "bus"
        viewModel.addMode(mockModeInfo)

        // Clear transport modes
        viewModel.clearTransportModes()

        // Verify
        assertTrue(viewModel.transportModes.isEmpty())
    }

    @Test
    fun `test setting locations updates the items list`() {
        val location1: NearbyLocation = mockk(relaxed = true)
        val location2: NearbyLocation = mockk(relaxed = true)

        every { location1.title } returns "Location 1"
        every { location1.address } returns "Address 1"
        every { location2.title } returns "Location 2"
        every { location2.address } returns "Address 2"

        val locations = listOf(location1, location2)

        // Execute
        viewModel.setLocations(locations)

        // Verify
        assertEquals(2, viewModel.items.size)
        assertEquals("Location 1", viewModel.items[0].title.get())
        assertEquals("Address 1", viewModel.items[0].location.get())

        assertEquals("Location 2", viewModel.items[1].title.get())
        assertEquals("Address 2", viewModel.items[1].location.get())

        assertTrue(viewModel.showModes.get())
    }
}
