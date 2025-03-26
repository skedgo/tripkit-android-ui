package com.skedgo.tripkit.ui.tripresults

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.TripKit
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.notification.cancelChannelNotifications
import com.skedgo.tripkit.routing.Availability
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import io.mockk.*
import io.reactivex.functions.Consumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.internal.assertFalse
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("""
    Runs fine when running only this test but failing when running but failing 
    when running along the other tests. 
    Will check later.
""")
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripResultViewModelTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripResultViewModel
    private val mockContext: Context = mockk(relaxed = true)
    private val mockResources: Resources = mockk(relaxed = true)
    private val printTime: PrintTime = mockk(relaxed = true)
    private val tripSegmentHelper: TripSegmentHelper = mockk(relaxed = true)
    private val transportModePrefs: TransportModeSharedPreference = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        DateTimeZone.setProvider(UTCProvider())
        initDispatchers()

        every { mockContext.resources } returns mockResources
        every { mockResources.getString(any()) } returns "Test"

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

        viewModel = TripResultViewModel(
            mockContext,
            tripSegmentHelper,
            printTime,
            mockResources,
            transportModePrefs
        )
    }

    @After
    fun tearDown() {
        tearDownDispatchers()
    }

    @Test
    fun `toggleShowMore shows other trips and updates flags`() {

        val mockErrorHandler = mockk<Consumer<Throwable>>(relaxed = true)

        val configs = mockk<Configs> {
            every { hideTripMetrics() } returns true
            every { hasTripLabels() } returns true
            every { context() } returns context
            every { errorHandler() } returns mockErrorHandler
        }

        TripKit.initialize(configs)

        val tripSegment = mockk<TripSegment>(relaxed = true) {
            every { availability } returns Availability.MissedPrebookingWindow.value
        }

        val fromLocation = mockk<Location>(relaxed = true) {
            every { displayName } returns "Home"
            every { timeZone } returns "UTC" // ✅ Valid timezone
        }

        val otherTrips = listOf<Trip>(mockk(relaxed = true) {
            every { from } returns fromLocation
            every { segmentList } returns mutableListOf(tripSegment)
        })
        val displayTrip = mockk<Trip>(relaxed = true) {
            every { startTimeInSecs } returns 100L
            every { from } returns fromLocation
            every { segmentList } returns mutableListOf(tripSegment)
        }

        val tripGroup = spyk(TripGroup())
        tripGroup.changeDisplayTrip(displayTrip)
        tripGroup.setTrips(ArrayList(otherTrips))
        tripGroup.setUuid("uuid-123")
        tripGroup.displayTripId = 1

        viewModel.setTripGroup(mockContext, tripGroup, TripGroupClassifier.Classification.NONE)

        assertFalse(viewModel.showMoreTrips.value == true)

        viewModel.toggleShowMore()

        assertTrue(viewModel.showMoreTrips.value == true)
        assertEquals("Test", viewModel.moreButtonText.value)
    }

    @Test
    fun `setTripGroup assigns group, trip and tripResults`() {

        val mockErrorHandler = mockk<Consumer<Throwable>>(relaxed = true)

        val configs = mockk<Configs> {
            every { hideTripMetrics() } returns true
            every { hasTripLabels() } returns true
            every { context() } returns context
            every { errorHandler() } returns mockErrorHandler
        }

        TripKit.initialize(configs)

        val tripSegment = mockk<TripSegment>(relaxed = true) {
            every { availability } returns Availability.MissedPrebookingWindow.value
        }

        val fromLocation = mockk<Location>(relaxed = true) {
            every { displayName } returns "Home"
            every { timeZone } returns "UTC" // ✅ Valid timezone
        }

        val displayTrip = mockk<Trip>(relaxed = true) {
            every { uuid } returns "trip-uuid"
            every { from } returns fromLocation
            every { segmentList } returns mutableListOf(tripSegment)
        }

        val tripGroup = spyk(TripGroup())
        tripGroup.changeDisplayTrip(displayTrip)
        tripGroup.setTrips(ArrayList(listOf(displayTrip)))
        tripGroup.setUuid("uuid-123")
        tripGroup.displayTripId = 1

        viewModel.setTripGroup(mockContext, tripGroup, TripGroupClassifier.Classification.NONE)

        assertEquals(tripGroup, viewModel.group)
        assertEquals(displayTrip, viewModel.trip)
        assertFalse(viewModel.badgeVisible.value ?: true)
        assertEquals(1, viewModel.tripResults.value?.size)
    }
}
