package com.skedgo.tripkit.ui.trippreview.external

import android.content.Context
import android.text.format.DateFormat
import android.webkit.URLUtil
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.common.model.booking.Booking
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trippreview.handleExternalAction
import com.skedgo.tripkit.ui.utils.SystemTimeFormatManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExternalActionTripPreviewItemViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ExternalActionTripPreviewItemViewModel
    private lateinit var mockContext: Context
    private lateinit var mockSegment: TripSegment
    private lateinit var mockBooking: Booking

    @Before
    fun setUp() {
        DateTimeZone.setDefault(DateTimeZone.UTC)
        mockContext = mockk(relaxed = true)
        mockBooking = mockk(relaxed = true)
        mockSegment = mockk(relaxed = true) {
            booking = mockBooking
        }

        // Mock static classes
        mockkStatic(URLUtil::class)
        every { URLUtil.isNetworkUrl(any()) } returns true
        
        mockkStatic(DateTimeZone::class)
        every { DateTimeZone.forID(any()) } answers { DateTimeZone.UTC }
        
        // Mock DateFormat.is24HourFormat to prevent NullPointerException
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(any()) } returns false
        
        // Mock SystemTimeFormatManager
        mockkObject(SystemTimeFormatManager)
        every { SystemTimeFormatManager.getTimeFormatPattern() } returns "h:mm a"
        every { SystemTimeFormatManager.getTimeFormatPatternWithAmPm() } returns "h:mm a"

        viewModel = ExternalActionTripPreviewItemViewModel()

        every { mockBooking.externalActions } returns mutableListOf(
            "gocatch",
            "ingogo",
            "tel:12345678",
            "https://example.com"
        )
        every { mockContext.getString(R.string.gocatch_a_taxi) } returns "GoCatch a Taxi"
        every { mockContext.getString(R.string.action_get_ingogo) } returns "Get Ingogo"
        every { mockContext.getString(R.string.action_call_taxis) } returns "Call a Taxi"
        every { mockContext.getString(R.string.show_website) } returns "Show Website"
        every { mockContext.handleExternalAction(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(URLUtil::class)
        unmockkStatic(DateTimeZone::class)
        unmockkStatic(DateFormat::class)
        unmockkObject(SystemTimeFormatManager)
    }

    @Test
    fun `setSegment correctly populates items`() {
        every { mockSegment.timeZone } returns "Australia/Sydney"

        viewModel.setSegment(mockContext, mockSegment)

        assertEquals(4, viewModel.items.size)
        assertEquals("GoCatch a Taxi", viewModel.items[0].title.get())
        assertEquals("Get Ingogo", viewModel.items[1].title.get())
        assertEquals("Call a Taxi", viewModel.items[2].title.get())
        assertEquals("Show Website", viewModel.items[3].title.get())
    }

    @Test
    fun `generateTitle returns correct title for known external actions`() {
        assertEquals("GoCatch a Taxi", viewModel.generateTitle(mockContext, "gocatch", mockBooking))
        assertEquals("Get Ingogo", viewModel.generateTitle(mockContext, "ingogo", mockBooking))
        assertEquals(
            "Call a Taxi",
            viewModel.generateTitle(mockContext, "tel:12345678", mockBooking)
        )
        assertEquals(
            "Show Website",
            viewModel.generateTitle(mockContext, "https://example.com", mockBooking)
        )
    }

    @Test
    fun `generateTitle returns booking title when only one action exists`() {
        every { mockBooking.externalActions } returns listOf("gocatch")
        every { mockBooking.title } returns "Booking Title"

        assertEquals("Booking Title", viewModel.generateTitle(mockContext, "gocatch", mockBooking))
    }
}
