package com.skedgo.tripkit.ui.controller.routeviewcontroller

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.location.Location
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
class TKUIRouteViewModelTest: MockKTest() {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule() // Ensures LiveData executes immediately

    private lateinit var viewModel: TKUIRouteViewModel
    private val startObserver: Observer<String> = mockk(relaxed = true)
    private val destinationObserver: Observer<String> = mockk(relaxed = true)

    @Before
    fun setup() {
        initRx()
        viewModel = TKUIRouteViewModel()
        viewModel.start.observeForever(startObserver)
        viewModel.destination.observeForever(destinationObserver)
    }

    @After
    fun teardown() {
        tearDownRx()
        clearAllMocks()
    }

    @Test
    fun `swap should trigger PublishRelay`() {
        val swapRelay = mockk<PublishRelay<Unit>>(relaxed = true)
        viewModel.swap = swapRelay

        viewModel.swap()

        verify { swapRelay.accept(Unit) }
    }

    @Test
    fun `bothLocationsAreValid should return true when both locations are valid`() {
        viewModel.startLocation = Location(12.34, 56.78).apply {
            name = "Start Location"
        }
        viewModel.destinationLocation = Location(23.45, 67.89).apply {
            name = "Destination Location"
        }

        assertTrue(viewModel.bothLocationsAreValid())
    }

    @Test
    fun `bothLocationsAreValid should return false when locations are invalid`() {
        viewModel.startLocation = null
        viewModel.destinationLocation = Location(0.0, 0.0).apply {
            name = "Destination Location"
        }

        assertFalse(viewModel.bothLocationsAreValid())
    }

    @Test
    fun `swapLocations should swap startLocation and destinationLocation`() {
        val startLoc = Location(12.34, 56.78).apply {
            name = "Start Location"
        }
        val destLoc = Location(23.45, 67.89).apply {
            name = "Destination Location"
        }

        viewModel.startLocation = startLoc
        viewModel.destinationLocation = destLoc

        viewModel.swapLocations()

        assertEquals(destLoc, viewModel.startLocation)
        assertEquals(startLoc, viewModel.destinationLocation)
    }

    @Test
    fun `setStart should update start LiveData`() {
        val startValue = "New Start Location"

        viewModel.setStart(startValue)

        verify { startObserver.onChanged(startValue) }
        assertEquals(startValue, viewModel.start.value)
    }

    @Test
    fun `setDestination should update destination LiveData`() {
        val destinationValue = "New Destination Location"

        viewModel.setDestination(destinationValue)

        verify { destinationObserver.onChanged(destinationValue) }
        assertEquals(destinationValue, viewModel.destination.value)
    }
}
