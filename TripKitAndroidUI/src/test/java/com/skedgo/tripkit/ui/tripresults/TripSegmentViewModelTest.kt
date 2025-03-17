package com.skedgo.tripkit.ui.tripresults

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.ContextCompat
import com.google.android.libraries.places.api.Places
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.HttpClientModule
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.configuration.Key.ApiKey
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.notification.cancelChannelNotifications
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.startDateTime
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.reactivex.Single
import io.reactivex.functions.Consumer
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripSegmentViewModelTest : MockKTest() {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var printTime: PrintTime
    private lateinit var transportModeSharedPreference: TransportModeSharedPreference
    private lateinit var resources: Resources
    private lateinit var viewModel: TripSegmentViewModel

    @Before
    fun setUp() {
        DateTimeZone.setProvider(UTCProvider())
        MockKAnnotations.init(this)

        initRx()
        val apiKey = ApiKey("a1s2d3f4g5h6")
        context = mockk(relaxed = true)
        printTime = mockk(relaxed = true)
        transportModeSharedPreference = mockk(relaxed = true)
        resources = mockk(relaxed = true) {
            every { getDrawable(any()) } returns mockk(relaxed = true)
        }

        every { context.resources } returns resources

        val sharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        val sharedPreferences = mockk<SharedPreferences> {
            every { getBoolean(any(), any()) } returns false
            every { edit() } returns sharedPreferencesEditor
        }

        val mockAlarmManager = mockk<AlarmManager>(relaxed = true)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager

        val mockNotificationManager = mockk<NotificationManager>(relaxed = true)

        mockkStatic("com.skedgo.tripkit.notification.NotificationKt")
        every { context.cancelChannelNotifications(any()) } just Runs

        every {
            context.getSystemService(NotificationManager::class.java)
        } returns mockNotificationManager

        every {
            mockNotificationManager.createNotificationChannels(any())
        } just Runs

        every {
            context.getSharedPreferences(any(), any())
        } returns sharedPreferences

        val mockErrorHandler = mockk<Consumer<Throwable>>(relaxed = true)
        val httpClientModule = mockk<HttpClientModule>(relaxed = true)

        val configs = mockk<Configs> {
            every { hideTripMetrics() } returns true
            every { hasTripLabels() } returns true
            every { debuggable() } returns true
            every { context() } returns context
            every { errorHandler() } returns mockErrorHandler
        }

        mockkStatic(Places::class)
        every { Places.isInitialized() } returns true
        every { Places.initialize(any(), any()) } returns Unit

        every { TripKitUI.buildTripKitConfig(context, apiKey) } returns configs

        TripKitUI.initialize(context, apiKey, configs, httpClientModule)

        viewModel = spyk(
            TripSegmentViewModel(context, printTime, transportModeSharedPreference),
            recordPrivateCalls = true
        )

        val transportTintStrategy = mockk<TransportTintStrategy>(relaxed = true) {
            every {
                apply(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } answers { arg(3) }
        }

        val getTransportIconTintStrategyMock = mockk<GetTransportIconTintStrategy>(relaxed = true) {
            every { this@mockk.invoke() } returns Single.just(transportTintStrategy)
        }

        viewModel.getTransportIconTintStrategy = getTransportIconTintStrategyMock
    }


    @After
    fun tearDown() {
        tearDownRx()
        unmockkAll()
    }

    @Test
    fun `setSegment sets primary text and shows title`() {
        val trip = mockk<Trip>(relaxed = true)
        val fromLocation = mockk<Location>(relaxed = true) {
            every { displayName } returns "Home"
            every { timeZone } returns "UTC"
        }
        val segment = mockk<TripSegment>(relaxed = true) {
            every { serviceNumber } returns "123"
            every { isHideExactTimes } returns false
            every { isRealTime } returns false
            every { bicycleAccessible } returns true
            every { hasTimeTable() } returns false
            every { from } returns fromLocation

            every { darkVehicleIconWithNoRealtimeChecking } returns 1

            every { alerts } returns null
        }

        every { transportModeSharedPreference.isTransportModeEnabled(any()) } returns true
        every { resources.getString(any(), any()) } returns "Cycle friendly"
        every { resources.getString(any()) } returns "Some string"

        viewModel.setSegment(trip, segment)

        assert(viewModel.primaryText.get() == "123")
        assert(viewModel.showPrimary.get())
    }

    @Test
    fun `buildSubtitle sets secondaryText with subtitle`() {
        val trip = mockk<Trip>(relaxed = true) {
            every { isMixedModal(any()) } returns false
        }

        val fromLocation = mockk<Location>(relaxed = true) {
            every { displayName } returns "Home"
            every { timeZone } returns "UTC" // ✅ Valid timezone
        }

        val segment = mockk<TripSegment>(relaxed = true) {
            every { isRealTime } returns true
            every { hasTimeTable() } returns true
            every { frequency } returns 0
            every { from } returns fromLocation
        }

        every { printTime.printLocalTime(any()) } returns "08:00 AM"

        viewModel.buildSubtitle(trip, segment)

        val subtitle = viewModel.secondaryText.get().toString()
        assert(subtitle.contains("08:00 AM"))
    }
}
