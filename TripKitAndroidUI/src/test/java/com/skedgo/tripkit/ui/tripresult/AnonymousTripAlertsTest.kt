package com.skedgo.tripkit.ui.tripresult

import android.app.AlarmManager
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.skedgo.TripKit
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.Callable

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnonymousTripAlertsTest : MockKTest() {
    @get:Rule val rule = InstantTaskExecutorRule()
    private lateinit var context: Context
    private lateinit var configs: Configs
    private lateinit var trip: Trip
    private lateinit var updater: TripUpdater
    private lateinit var reminders: RemindersRepository
    private lateinit var vm: TripSegmentGetOffAlertsViewModel
    private val alarms get() = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)

    @Before fun setup() {
        initRx()
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("KEY_GET_OFF_ALERTS", 0).edit().clear().commit()
        GetOffAlertCache.init(context)
        GetOffAlertCache.onTripAlertStateChanged = {}
        configs = mockk(relaxed = true)
        every { configs.context() } returns context
        every { configs.hasGetOffAlerts() } returns true
        every { configs.userTokenProvider() } returns Callable { "" }
        TripKit.initialize(configs)
        val kit = mockk<TripKit>()
        every { kit.configs() } returns configs
        mockkObject(TripKit.Companion)
        every { TripKit.getInstance() } returns kit
        mockkObject(GeoLocation)
        every { GeoLocation.clearGeofences() } just Runs
        every { GeoLocation.createGeoFences(any(), any(), any()) } just Runs
        updater = mockk()
        reminders = mockk()
        coEvery { reminders.getTripNotificationReminderMinutes() } returns 5L
        trip = Trip().apply {
            tripId = 42
            endTimeInSecs = System.currentTimeMillis() / 1000 + 4200
            segmentList = arrayListOf(TripSegment().apply {
                startTimeInSecs = System.currentTimeMillis() / 1000 + 3600
                endTimeInSecs = startTimeInSecs + 600
            })
        }
        vm = TripSegmentGetOffAlertsViewModel(trip, false, updater, reminders)
    }

    @After fun cleanup() { unmockkAll(); tearDownRx() }

    // Exercise the permission-approved entry point without mocking away alarms or persistence.
    private fun enable() {
        TripSegmentGetOffAlertsViewModel::class.java.getDeclaredMethod("enableAlerts", Context::class.java)
            .apply { isAccessible = true }.invoke(vm, context)
    }
    private fun remote(token: String = "test-token") {
        trip.subscribeURL = "https://example.invalid/subscribe"
        trip.updateURL = "https://example.invalid/update"
        every { configs.userTokenProvider() } returns Callable { token }
        every { updater.getUpdateAsync(any<String>()) } returns Observable.just(trip)
    }

    @Test fun `guest without subscribe URL persists local alerts and never calls server`() {
        enable()
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())).isTrue()
        assertThat(GetOffAlertCache.getTripId()).isEqualTo(trip.getTripUuid())
        verify(exactly = 0) { updater.tripSubscription(any()) }
    }
    @Test fun `guest trip start alarm has the correct trigger and identity`() {
        enable()
        val alarm = alarms.scheduledAlarms.single()
        assertThat(alarm.triggerAtTime).isEqualTo((trip.segmentList.first().startTimeInSecs - 300) * 1000)
        assertThat(shadowOf(alarm.operation).savedIntent.getBooleanExtra(TripAlarmBroadcastReceiver.ACTION_START_TRIP_EVENT, false)).isTrue()
        assertThat(shadowOf(alarm.operation).savedIntent.getStringExtra(TripAlarmBroadcastReceiver.EXTRA_START_TRIP_EVENT_TRIP)).contains("42")
    }
    @Test fun `local alert state is restored when the trip is reopened`() {
        enable()
        val restored = TripSegmentGetOffAlertsViewModel(trip, false, updater, reminders)
        assertThat(restored.getOffAlertStateOn.value).isTrue()
    }
    @Test fun `mute removes local alarm and persisted state`() {
        enable()
        vm.onAlertChange(context, false)
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())).isFalse()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        verify(atLeast = 1) { GeoLocation.clearGeofences() }
    }
    @Test fun `authenticated server success enables alerts`() {
        remote()
        every { updater.tripSubscription(trip.subscribeURL!!) } returns Observable.just(Any())
        enable()
        verify(exactly = 1) { updater.tripSubscription(trip.subscribeURL!!) }
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
    }
    @Test fun `cached subscribe URL without token cannot claim successful activation`() {
        remote("")
        every { updater.tripSubscription(any()) } returns Observable.error(IllegalStateException("missing token"))
        enable()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        verify(exactly = 0) { updater.tripSubscription(any()) }
    }
    @Test fun `server failure does not fall back to local activation`() {
        remote()
        every { updater.tripSubscription(any()) } returns Observable.error(IllegalStateException("subscription failed"))
        enable()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }
    @Test fun `alerts remain off until server acknowledges subscription`() {
        remote()
        val response = PublishSubject.create<Any>()
        every { updater.tripSubscription(any()) } returns response
        enable()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        response.onNext(Any())
        assertThat(vm.getOffAlertStateOn.value).isTrue()
    }
    @Test fun `local geofences are registered with the selected trip and timeline`() {
        val fence = Geofence("arrival", "circle", "ENTER", Coordinate(-35.28, 149.13), 100.0, "TRIP_END", "Arrival", "Arrived")
        trip.segmentList.first().geofences = listOf(fence)
        enable()
        verify { GeoLocation.createGeoFences(trip, match { it.single() == fence && it.single().timeline > 0 }, any()) }
    }
    private fun details(): List<TripSegmentGetOffAlertDetailViewModel> {
        val segments = TripSegmentsViewModel(context, mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), updater, reminders,
            mockk(relaxed = true), mockk(relaxed = true))
        val method = TripSegmentsViewModel::class.java.getDeclaredMethod("setupGetOffAlert", TripGroup::class.java, Trip::class.java)
        method.isAccessible = true
        return (method.invoke(segments, TripGroup(), trip) as TripSegmentGetOffAlertsViewModel).items.toList()
    }
    @Test fun `vehicle approach type is absent even with old geofence when subscribe URL is missing`() {
        trip.segmentList.first().geofences = listOf(Geofence("boarding", "circle", "ENTER", Coordinate(-35.28, 149.13), 100.0, "VEHICLE_IS_APPROACHING", "Board", "Board"))
        assertThat(details().map { it.title }).doesNotContain(context.getString(com.skedgo.tripkit.ui.R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop))
    }
    @Test fun `vehicle approach type is offered when subscribe URL is present`() {
        remote()
        assertThat(details().filter { it.isEnabled }.map { it.title }).contains(context.getString(com.skedgo.tripkit.ui.R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop))
    }
    @Test fun `trip without local eligible types does not offer any local type`() {
        trip.segmentList.first().startTimeInSecs = 1
        assertThat(details().filter { it.isEnabled }).isEmpty()
    }

    @Test fun `blank subscribe URL behaves as local only`() {
        trip.subscribeURL = " "
        enable()
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        verify(exactly = 0) { updater.tripSubscription(any()) }
    }
    @Test fun `duplicate enable does not schedule a second alarm`() {
        enable()
        enable()
        assertThat(alarms.scheduledAlarms).hasSize(1)
    }
    @Test fun `empty subscription response cannot claim success`() {
        remote()
        every { updater.tripSubscription(any()) } returns Observable.empty()
        enable()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }
    @Test fun `malformed server URL failure cannot claim success`() {
        remote()
        trip.subscribeURL = "not a URL"
        every { updater.tripSubscription(any()) } returns Observable.error(IllegalArgumentException("Invalid URL"))
        enable()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }


    @Test fun `mute cancels pending remote activation before a delayed acknowledgement`() {
        remote()
        val response = PublishSubject.create<Any>()
        every { updater.tripSubscription(any()) } returns response
        enable()
        vm.onAlertChange(context, false)
        response.onNext(Any())
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(response.hasObservers()).isFalse()
    }

    @Test fun `replaced alert owner cannot activate from an old server response`() {
        remote()
        val response = PublishSubject.create<Any>()
        every { updater.tripSubscription(any()) } returns response
        enable()
        vm.disposeAlerts()
        response.onNext(Any())
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(response.hasObservers()).isFalse()
    }

    @Test fun `disposed owner ignores a late permission approval`() {
        vm.disposeAlerts()
        enable()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        verify(exactly = 0) { updater.tripSubscription(any()) }
    }

    @Test fun `guest activates through disclosure with normal granted OS permissions`() {
        val controller = org.robolectric.Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java)
        controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        val activity = controller.setup().get()
        shadowOf(activity.application).grantPermissions(
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        val location = activity.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        shadowOf(location).setProviderEnabled(android.location.LocationManager.GPS_PROVIDER, true)
        vm.onAlertChange(activity, true)
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        dialog.findViewById<android.view.View>(com.afollestad.materialdialogs.R.id.md_button_positive).performClick()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
        controller.pause().stop().destroy()
    }

    @Test fun `same trip realtime refresh preserves pending alert activation`() {
        val segments = TripSegmentsViewModel(context, mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), updater, reminders,
            mockk(relaxed = true), mockk(relaxed = true))
        val setup = TripSegmentsViewModel::class.java.getDeclaredMethod("setupGetOffAlert", TripGroup::class.java, Trip::class.java)
            .apply { isAccessible = true }
        vm = setup.invoke(segments, TripGroup(), trip) as TripSegmentGetOffAlertsViewModel
        TripSegmentsViewModel::class.java.getDeclaredField("tripSegmentGetOffAlertsViewModel")
            .apply { isAccessible = true }.set(segments, vm)
        remote()
        val response = PublishSubject.create<Any>()
        every { updater.tripSubscription(any()) } returns response
        enable()
        val updatedTrip = com.google.gson.Gson().fromJson(com.google.gson.Gson().toJson(trip), Trip::class.java)
        updatedTrip.segmentList.first().startTimeInSecs += 60
        val refreshed = setup.invoke(segments, TripGroup(), updatedTrip)
        assertThat(refreshed).isSameAs(vm)
        assertThat(vm.trip).isSameAs(updatedTrip)
        vm.alertStateListener = {}
        response.onNext(Any())
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
        assertThat(alarms.scheduledAlarms.single().triggerAtTime)
            .isEqualTo((updatedTrip.segmentList.first().startTimeInSecs - 300) * 1000)
    }

}
