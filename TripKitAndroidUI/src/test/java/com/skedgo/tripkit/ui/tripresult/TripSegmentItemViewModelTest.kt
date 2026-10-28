package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Occupancy
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.tripresults.TripSegmentHelper
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class TripSegmentItemViewModelTest : MockKTest() {

    private lateinit var context: Context

    @MockK
    private lateinit var occupancyViewModel: OccupancyViewModel

    @MockK
    private lateinit var getTransportIconTintStrategy: GetTransportIconTintStrategy

    @MockK
    private lateinit var tripSegmentHelper: TripSegmentHelper

    @MockK
    private lateinit var printTime: PrintTime

    @MockK(relaxed = true)
    private lateinit var transportModeSharedPreference: TransportModeSharedPreference

    private lateinit var viewModel: TripSegmentItemViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mockTripSegment = mockk<TripSegment>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        val mockResources = mockk<Resources>().apply {
            every { getColor(any()) } returns mockk()
        }

        context = mockk<Context>().apply {
            every { resources } returns mockResources
            every { getColor(any()) } returns mockk()
        }

        every { mockTripSegment.realTimeVehicle = any() } just Runs

        viewModel = TripSegmentItemViewModel(
            context,
            getTransportIconTintStrategy,
            tripSegmentHelper,
            printTime,
            occupancyViewModel,
            transportModeSharedPreference
        )

    }

    @Test
    fun `initOccupancy - should call setOccupancy if TripSegment realTimeVehicle is not null`() {
        val realTimeVehicle = mockk<RealTimeVehicle>()
        mockTripSegment.realTimeVehicle = realTimeVehicle
        viewModel.initOccupancy(mockTripSegment)

        verify { occupancyViewModel.setOccupancy(mockTripSegment.realTimeVehicle!!, any()) }
    }

    @Test
    fun `initOccupancy - should not call setOccupancy if TripSegment realTimeVehicle is null`() {
        every { mockTripSegment.realTimeVehicle } returns null
        viewModel.initOccupancy(mockTripSegment)
        verify(exactly = 0) { occupancyViewModel.setOccupancy(any(), any()) }
    }

    @Test
    fun `updateAlertStateForViewType - shows deduplicated alerts for moving segment`() {
        val firstAlert = mockk<RealtimeAlert>()
        val duplicateFirstAlert = mockk<RealtimeAlert>()
        val secondAlert = mockk<RealtimeAlert>()
        every { firstAlert.title() } returns "Weekday track closure"
        every { duplicateFirstAlert.title() } returns "Weekday track closure"
        every { secondAlert.title() } returns "Other disruption"

        viewModel.updateAlertStateForViewType(
            TripSegmentItemViewModel.SegmentViewType.MOVING,
            listOf(firstAlert, duplicateFirstAlert, secondAlert)
        )

        assertTrue(viewModel.showAlerts.value == true)
        assertEquals(2, viewModel.alerts.value?.size)
        assertEquals(firstAlert, viewModel.alerts.value?.first())
        assertEquals(secondAlert, viewModel.alerts.value?.get(1))
    }

    @Test
    fun `updateAlertStateForViewType - hides alerts for stationary bridge segment`() {
        val alert = mockk<RealtimeAlert>()
        every { alert.title() } returns "Weekday track closure"

        viewModel.updateAlertStateForViewType(
            TripSegmentItemViewModel.SegmentViewType.STATIONARY_BRIDGE,
            listOf(alert)
        )

        assertFalse(viewModel.showAlerts.value == true)
        assertTrue(viewModel.alerts.value?.isEmpty() == true)
    }


    private fun timelineContext(night: Boolean): Context {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val configuration = android.content.res.Configuration(app.resources.configuration).apply {
            uiMode = if (night) android.content.res.Configuration.UI_MODE_NIGHT_YES
                else android.content.res.Configuration.UI_MODE_NIGHT_NO
        }
        return android.view.ContextThemeWrapper(app.createConfigurationContext(configuration), R.style.AppTheme)
    }

    private fun timelineModel(themed: Context): TripSegmentItemViewModel {
        val helper = mockk<TripSegmentHelper>()
        every { helper.getIconUrlForModeInfo(any(), any()) } returns null
        return TripSegmentItemViewModel(themed, mockk(relaxed = true), helper, mockk(relaxed = true),
            OccupancyViewModel(themed), TransportModeSharedPreference(themed))
    }

    private fun renderTimelineIcon(themed: Context, drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
        val image = android.widget.ImageView(themed).apply { setImageDrawable(drawable) }
        image.measure(android.view.View.MeasureSpec.makeMeasureSpec(128, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(128, android.view.View.MeasureSpec.EXACTLY))
        image.layout(0, 0, 128, 128)
        return android.graphics.Bitmap.createBitmap(128, 128, android.graphics.Bitmap.Config.ARGB_8888).also {
            it.eraseColor(themed.getColor(R.color.white))
            image.draw(android.graphics.Canvas(it))
        }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `standalone timeline travel and location icons are readable without changing light rendering`() {
        initRx()
        try {
            for (night in listOf(false, true)) {
                val themed = timelineContext(night)
                val model = timelineModel(themed)
                try {
                    for (mode in listOf("wheelchair", "walk", "bicycle", "arrival", "departure")) {
                        val segment = TripSegment().apply {
                            setType(when (mode) {
                                "arrival" -> com.skedgo.tripkit.routing.SegmentType.ARRIVAL
                                "departure" -> com.skedgo.tripkit.routing.SegmentType.DEPARTURE
                                else -> com.skedgo.tripkit.routing.SegmentType.UNSCHEDULED
                            })
                            modeInfo = com.skedgo.tripkit.routing.ModeInfo().apply { localIconName = mode }
                        }
                        model.tripSegment = segment
                        for (cancelled in listOf(false, true)) {
                            model.setupSegment(TripSegmentItemViewModel.SegmentViewType.MOVING, "Timeline", isCancelled = cancelled)
                            val binding = com.skedgo.tripkit.ui.databinding.TripSegmentBinding.inflate(android.view.LayoutInflater.from(themed))
                            binding.viewModel = model
                            binding.executePendingBindings()
                            val icon = binding.root.findViewById<android.widget.ImageView>(R.id.icon)
                            assertEquals(1f, icon.alpha)
                            assertTrue(icon.isEnabled)
                            var parent = icon.parent as? android.view.View
                            while (parent != null) { assertEquals(1f, parent.alpha); parent = parent.parent as? android.view.View }
                            val rendered = renderTimelineIcon(themed, icon.drawable!!)
                            val surface = themed.getColor(R.color.white)
                            val pixels = IntArray(128 * 128)
                            rendered.getPixels(pixels, 0, 128, 0, 0, 128, 128)
                            assertTrue("$mode night=$night must render readable timeline pixels",
                                pixels.count { androidx.core.graphics.ColorUtils.calculateContrast(it, surface) >= 3.0 } > 100)
                            if (!night) {
                                val source = ContextCompat.getDrawable(themed, if (mode == "arrival" || mode == "departure") R.drawable.v4_ic_map_location else segment.darkVehicleIcon)!!
                                assertTrue("Light $mode must be unchanged", rendered.sameAs(renderTimelineIcon(themed, source)))
                            }
                        }
                    }
                } finally { model.onCleared() }
            }
        } finally { tearDownRx() }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `timeline keeps route circle white icons and standalone branding untinted`() {
        initRx()
        try {
            for (night in listOf(false, true)) {
                val themed = timelineContext(night)
                val model = timelineModel(themed)
                try {
                    val segment = TripSegment().apply {
                        setType(com.skedgo.tripkit.routing.SegmentType.UNSCHEDULED)
                        modeInfo = com.skedgo.tripkit.routing.ModeInfo().apply { localIconName = "bus" }
                    }
                    model.tripSegment = segment
                    model.setupSegment(TripSegmentItemViewModel.SegmentViewType.MOVING, "Bus", lineColor = android.graphics.Color.BLUE)
                    val source = ContextCompat.getDrawable(themed, segment.darkVehicleIcon)!!.mutate()
                    source.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
                    assertTrue(renderTimelineIcon(themed, source).sameAs(renderTimelineIcon(themed, model.icon.value!!)))
                    assertEquals(android.graphics.Color.BLUE, model.backgroundCircleTint.value)
                    segment.modeInfo!!.remoteIconIsBranding = true
                    model.setupSegment(TripSegmentItemViewModel.SegmentViewType.MOVING, "Brand")
                    val original = ContextCompat.getDrawable(themed, segment.darkVehicleIcon)!!
                    assertTrue(renderTimelineIcon(themed, original).sameAs(renderTimelineIcon(themed, model.icon.value!!)))
                } finally { model.onCleared() }
            }
        } finally { tearDownRx() }
    }
}