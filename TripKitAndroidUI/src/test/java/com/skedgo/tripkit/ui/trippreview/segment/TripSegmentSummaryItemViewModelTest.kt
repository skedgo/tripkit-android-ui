package com.skedgo.tripkit.ui.trippreview.segment

import android.graphics.drawable.Drawable
import com.skedgo.tripkit.common.model.TransportMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import io.mockk.*
import org.junit.After
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TripSegmentSummaryItemViewModelTest {

    private lateinit var mockDrawable: Drawable

    @Before
    fun setup() {
        mockDrawable = mockk(relaxed = true)

        // Mock TransportMode.getLocalIconResId to return 0 by default
        mockkObject(TransportMode)
        every { TransportMode.getLocalIconResId(any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `parseFromTripSegmentSummary sets values correctly`() {
        // Arrange
        val summary = TripSegmentSummary(
            id = 123L,
            title = "Trip Title",
            subTitle = "Trip Subtitle",
            icon = mockDrawable,
            description = "Trip Description",
            modeId = TransportMode.ID_TAXI
        )

        val isRightToLeft = true

        // Act
        val viewModel = TripSegmentSummaryItemViewModel.parseFromTripSegmentSummary(
            summary,
            isRightToLeft
        )

        // Assert
        assertEquals(123L, viewModel.id.get())
        assertEquals("Trip Title", viewModel.title.get())
        assertEquals("Trip Subtitle", viewModel.subTitle.get())
        assertEquals(mockDrawable, viewModel.icon.get())
        assertEquals("Trip Description", viewModel.description.get())
        assertEquals(TransportMode.ID_TAXI, viewModel.modeId.get())

        // Taxi mode should not mirror
        assertFalse(viewModel.isMirrored.get())
    }

    @Test
    fun `parseFromTripSegmentSummary sets isMirrored true for custom me_car-r`() {
        // Arrange
        val summary = TripSegmentSummary(
            id = 456L,
            title = "Car Ride",
            subTitle = "Custom Subtitle",
            icon = mockDrawable,
            description = "Car Ride Description",
            modeId = "me_car-r"
        )

        val isRightToLeft = true

        // Act
        val viewModel = TripSegmentSummaryItemViewModel.parseFromTripSegmentSummary(
            summary,
            isRightToLeft
        )

        // Assert
        assertTrue(viewModel.isMirrored.get())
    }

    @Test
    fun `parseFromTripSegmentSummary sets isMirrored true for non-taxi mode with local icon`() {
        // Arrange
        every { TransportMode.getLocalIconResId("pt_bus") } returns 123 // non-zero, will trigger mirroring

        val summary = TripSegmentSummary(
            id = 789L,
            title = "Bus Ride",
            subTitle = "Bus Subtitle",
            icon = mockDrawable,
            description = "Bus Ride Description",
            modeId = "pt_bus"
        )

        val isRightToLeft = true

        // Act
        val viewModel = TripSegmentSummaryItemViewModel.parseFromTripSegmentSummary(
            summary,
            isRightToLeft
        )

        // Assert
        assertTrue(viewModel.isMirrored.get())
    }
}
