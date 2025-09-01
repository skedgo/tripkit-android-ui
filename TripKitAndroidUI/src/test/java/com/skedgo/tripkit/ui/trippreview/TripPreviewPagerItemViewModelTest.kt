package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.SegmentType.ARRIVAL
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.endDateTime
import com.skedgo.tripkit.routing.startDateTime
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.utils.SystemTimeFormatManager
import io.mockk.*
import org.amshove.kluent.internal.assertEquals
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripPreviewPagerItemViewModelTest: MockKTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripPreviewPagerItemViewModel
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources

    @Before
    fun setUp() {
        DateTimeZone.setProvider(UTCProvider())
        initRx()

        viewModel = TripPreviewPagerItemViewModel()

        mockContext = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)

        every { mockContext.resources } returns mockResources

        mockkStatic(TripSegmentUtils::class)
        every { TripSegmentUtils.getTripSegmentAction(any(), any()) } returns "Mocked Action"

        mockkStatic(TransportModeUtils::class)
        every { TransportModeUtils.getIconUrlForModeInfo(any(), any()) } returns "https://mock.url/icon.png"

        mockkStatic(ContextCompat::class)
        every { ContextCompat.getDrawable(any(), any()) } returns mockk(relaxed = true)

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { false }

        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(any()) } returns false

        mockkObject(SystemTimeFormatManager)
        every { SystemTimeFormatManager.getTimeFormatPattern() } returns "h:mm a"
        every { SystemTimeFormatManager.getTimeFormatPatternWithAmPm() } returns "h:mm a"

        mockkStatic("android.text.format.DateUtils")

        every {
            android.text.format.DateUtils.isToday(any())
        } returns false

        mockkStatic("com.skedgo.tripkit.routing.TripSegmentExtensionsKt")
    }

    @After
    fun tearDown() {
        tearDownRx()
        unmockkObject(SystemTimeFormatManager)
        unmockkStatic(DateFormat::class)
        unmockkAll()
    }

    @Test
    fun `setSegment should update title, notes and other fields`() {

        val tripSegment = spyk(TripSegment())
        tripSegment.modeInfo = null
        tripSegment.transportModeId = "bus"
        tripSegment.metres = 1000
        tripSegment.setType(ARRIVAL)

        every { tripSegment.serviceNumber } returns "1234"
        every { tripSegment.getType() } returns ARRIVAL
        every { tripSegment.startTimeInSecs } returns DateTime.now().minusHours(1).millis / 1000
        every { tripSegment.endTimeInSecs } returns DateTime.now().millis / 1000
        every { tripSegment.startDateTime } returns DateTime.now(DateTimeZone.UTC)
        every { tripSegment.endDateTime } returns DateTime.now(DateTimeZone.UTC)
        every { tripSegment.isHideExactTimes } returns false

        val mockFromLocation = mockk<Location>(relaxed = true)
        val mockToLocation = mockk<Location>(relaxed = true)

        every { mockFromLocation.address } returns "Start Address"
        every { mockToLocation.address } returns "End Address"

        every { tripSegment.from } returns mockFromLocation
        every { tripSegment.to } returns mockToLocation

        viewModel.setSegment(mockContext, tripSegment)

        assertEquals("Mocked Action", viewModel.title.get())
        assertEquals("Start Address", viewModel.fromLocation.get())
        assertEquals("End Address", viewModel.toLocation.get())
        assertNotNull(viewModel.icon.get())
        assertEquals("0.6 mi", viewModel.notes.get())
        assertEquals("https://mock.url/icon.png", viewModel.modeIconUrl.get())
    }
}
