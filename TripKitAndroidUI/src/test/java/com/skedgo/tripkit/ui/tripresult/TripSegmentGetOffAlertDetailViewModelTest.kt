package com.skedgo.tripkit.ui.tripresult

import android.graphics.drawable.Drawable
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class TripSegmentGetOffAlertDetailViewModelTest {

    @Test
    fun `areItemsTheSame returns true when titles are the same`() {
        val item1 = TripSegmentGetOffAlertDetailViewModel(mockk(), "Title1", true)
        val item2 = TripSegmentGetOffAlertDetailViewModel(mockk(), "Title1", true)

        val result = TripSegmentGetOffAlertDetailViewModel.diffCallback()
            .areItemsTheSame(item1, item2)

        assertTrue(result)
    }

    @Test
    fun `areItemsTheSame returns false when titles are different`() {
        val item1 = TripSegmentGetOffAlertDetailViewModel(mockk(), "Title1", true)
        val item2 = TripSegmentGetOffAlertDetailViewModel(mockk(), "Title2", true)

        val result = TripSegmentGetOffAlertDetailViewModel.diffCallback()
            .areItemsTheSame(item1, item2)

        assertFalse(result)
    }

    @Test
    fun `areContentsTheSame returns true when icon and title are the same`() {
        val drawable = mockk<Drawable>()
        val item1 = TripSegmentGetOffAlertDetailViewModel(drawable, "Title1", true)
        val item2 = TripSegmentGetOffAlertDetailViewModel(drawable, "Title1", true)

        val result = TripSegmentGetOffAlertDetailViewModel.diffCallback()
            .areContentsTheSame(item1, item2)

        assertTrue(result)
    }

    @Test
    fun `areContentsTheSame returns false when icons are different`() {
        val item1 = TripSegmentGetOffAlertDetailViewModel(mockk(), "Title1", true)
        val item2 = TripSegmentGetOffAlertDetailViewModel(mockk(), "Title1", true)

        val result = TripSegmentGetOffAlertDetailViewModel.diffCallback()
            .areContentsTheSame(item1, item2)

        assertFalse(result)
    }

    @Test
    fun `areContentsTheSame returns false when titles are different`() {
        val drawable = mockk<Drawable>()
        val item1 = TripSegmentGetOffAlertDetailViewModel(drawable, "Title1", true)
        val item2 = TripSegmentGetOffAlertDetailViewModel(drawable, "Title2", true)

        val result = TripSegmentGetOffAlertDetailViewModel.diffCallback()
            .areContentsTheSame(item1, item2)

        assertFalse(result)
    }
}
