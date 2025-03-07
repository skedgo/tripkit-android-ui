package com.skedgo.tripkit.ui.trippreview.nearby

import android.webkit.URLUtil
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.LocationInfoService
import com.skedgo.tripkit.common.model.SharedVehicle
import com.skedgo.tripkit.common.model.SharedVehicleType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import io.mockk.*
import org.joda.time.DateTimeZone
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ModeLocationTripPreviewViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ModeLocationTripPreviewViewModel
    private val mockLocationInfoService: LocationInfoService = mockk(relaxed = true)

    @Before
    fun setUp() {

        mockkStatic(URLUtil::class)
        every { URLUtil.isNetworkUrl(any()) } returns true
        mockkStatic(DateTimeZone::class)
        every { DateTimeZone.forID(any()) } answers { DateTimeZone.UTC }

        viewModel = ModeLocationTripPreviewViewModel(mockLocationInfoService)
    }

    @Test
    fun `test setting segment with shared vehicle updates info groups`() {
        // Mock segment and shared vehicle
        val mockSegment: TripSegment = mockk(relaxed = true)
        val mockVehicle: SharedVehicle = mockk(relaxed = true)

        every { mockSegment.sharedVehicle } returns mockVehicle
        every { mockVehicle.vehicleType() } returns SharedVehicleType.CAR
        every { mockVehicle.name() } returns "Test Car"
        every { mockVehicle.batteryRange() } returns 50
        every { mockVehicle.operator()?.website } returns "https://test-operator.com"

        // Execute
        viewModel.set(mockSegment)

        // Verify
        assertEquals(2, viewModel.infoGroups.size)
        assertEquals(R.string.car, viewModel.infoGroups[0].title.get())
        assertEquals("Test Car", viewModel.infoGroups[0].value.get())

        assertEquals(R.string.battery, viewModel.infoGroups[1].title.get())
        assertEquals(DistanceFormatter.format(50 * 1000), viewModel.infoGroups[1].value.get())
    }

    @Test
    fun `test setting segment updates website`() {
        val mockSegment: TripSegment = mockk(relaxed = true)
        val mockVehicle: SharedVehicle = mockk(relaxed = true)

        every { mockSegment.sharedVehicle } returns mockVehicle
        every { mockVehicle.operator()?.website } returns "https://test-website.com"

        // Execute
        viewModel.set(mockSegment)

        // Verify
        assertTrue(viewModel.showWebsite.get())
        assertEquals("https://test-website.com", viewModel.website.get())
    }

    @Test
    fun `test setting location updates address and website`() {
        val location: NearbyLocation = mockk(relaxed = true)

        every { location.address } returns "123 Test St"
        every { location.website } returns "https://test-location.com"

        // Execute
        viewModel.set(location)

        // Verify
        assertTrue(viewModel.showAddress.get())
        assertEquals("123 Test St", viewModel.address.get())

        assertTrue(viewModel.showWebsite.get())
        assertEquals("https://test-location.com", viewModel.website.get())
    }
}
