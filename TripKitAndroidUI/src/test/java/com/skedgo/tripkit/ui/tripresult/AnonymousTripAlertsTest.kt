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
    @Test fun `vehicle approach type is visibly disabled even with old geofence when subscribe URL is missing`() {
        trip.segmentList.first().geofences = listOf(Geofence("boarding", "circle", "ENTER", Coordinate(-35.28, 149.13), 100.0, "VEHICLE_IS_APPROACHING", "Board", "Board"))
        assertThat(details().single { it.title == context.getString(com.skedgo.tripkit.ui.R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop) }.isEnabled).isFalse()
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
    @Test fun `malformed subscribe URL offers only local alerts without a server request`() {
        remote()
        trip.subscribeURL = "not a URL"
        enable()
        verify(exactly = 0) { updater.tripSubscription(any()) }
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
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

    @Test fun `availability changes invalidate the row contents`() {
        val enabled = TripSegmentGetOffAlertDetailViewModel(null, "Boarding", true)
        val disabled = TripSegmentGetOffAlertDetailViewModel(null, "Boarding", false)
        val diff = TripSegmentGetOffAlertDetailViewModel.diffCallback()
        assertThat(diff.areItemsTheSame(enabled, disabled)).isTrue()
        assertThat(diff.areContentsTheSame(enabled, disabled)).isFalse()
    }

    private fun activity(): org.robolectric.android.controller.ActivityController<androidx.appcompat.app.AppCompatActivity> {
        val controller = org.robolectric.Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java)
        controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        controller.setup()
        shadowOf(controller.get().application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(controller.get().application).denyPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val location = controller.get().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        shadowOf(location).setProviderEnabled(android.location.LocationManager.GPS_PROVIDER, true)
        trip.segmentList.first().geofences = listOf(Geofence("arrival", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"TRIP_END","Arrive","Arrive"))
        return controller
    }
    private fun positive(dialog: android.app.Dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()) {
        dialog.findViewById<android.view.View>(com.afollestad.materialdialogs.R.id.md_button_positive).performClick()
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }
    @Test fun `repeated enable during disclosure shows only one prompt`() {
        val c = activity()
        vm.onAlertChange(c.get(), true)
        val first = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        vm.onAlertChange(c.get(), true)
        assertThat(org.robolectric.shadows.ShadowDialog.getLatestDialog()).isSameAs(first)
        vm.disposeAlerts(); c.pause().stop().destroy()
    }
    @Test fun `mute during disclosure prevents late approval from activating`() {
        val c = activity()
        shadowOf(c.get().application).grantPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        vm.onAlertChange(c.get(), true)
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        vm.onAlertChange(c.get(), false)
        positive(dialog)
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        c.pause().stop().destroy()
    }
    @Test fun `while in use shows background rationale before opening settings`() {
        val c = activity()
        vm.onAlertChange(c.get(), true)
        positive()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertThat(dialog.isShowing).isTrue()
        assertThat(dialog.findViewById<android.widget.TextView>(com.afollestad.materialdialogs.R.id.md_text_title).text.toString())
            .isEqualTo(c.get().getString(com.skedgo.tripkit.ui.R.string.confirmation_allow_background_location_title))
        assertThat(shadowOf(c.get()).nextStartedActivity).isNull()
        dialog.cancel(); vm.disposeAlerts(); c.pause().stop().destroy()
    }

    @Test fun `disabled row binds grey text and disabled accessibility without click action`() {
        val c = activity()
        val binding = com.skedgo.tripkit.ui.databinding.ItemAlertDetailBinding.inflate(android.view.LayoutInflater.from(c.get()))
        binding.viewModel = TripSegmentGetOffAlertDetailViewModel(null, "Boarding", false)
        binding.executePendingBindings()
        assertThat(binding.root.isEnabled).isFalse()
        assertThat(binding.tvTitle.isEnabled).isFalse()
        assertThat(binding.tvTitle.currentTextColor).isEqualTo(androidx.core.content.ContextCompat.getColor(c.get(), com.skedgo.tripkit.ui.R.color.disabled))
        assertThat(binding.tvTitle.createAccessibilityNodeInfo().isEnabled).isFalse()
        assertThat(binding.root.hasOnClickListeners()).isFalse()
        assertThat(binding.root.isSelected).isFalse()
        binding.viewModel = TripSegmentGetOffAlertDetailViewModel(null, "Boarding", true)
        binding.executePendingBindings()
        assertThat(binding.tvTitle.isEnabled).isTrue()
        c.pause().stop().destroy()
    }
    @Test fun `absent present absent URL availability updates without affecting local rows`() {
        val title = context.getString(com.skedgo.tripkit.ui.R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop)
        assertThat(details().single { it.title == title }.isEnabled).isFalse()
        remote()
        assertThat(details().single { it.title == title }.isEnabled).isTrue()
        trip.subscribeURL = null
        assertThat(details().single { it.title == title }.isEnabled).isFalse()
        assertThat(details().first().isEnabled).isTrue()
    }
    @Test fun `cancel background explanation aborts and explicit retry opens once`() {
        val c = activity()
        vm.onAlertChange(c.get(), true); positive()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        dialog.findViewById<android.view.View>(com.afollestad.materialdialogs.R.id.md_button_negative).performClick()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        vm.validate()
        assertThat(dialog.isShowing).isFalse()
        vm.onAlertChange(c.get(), true)
        assertThat(org.robolectric.shadows.ShadowDialog.getLatestDialog()).isNotSameAs(dialog)
        vm.disposeAlerts(); c.pause().stop().destroy()
    }
    @Test fun `back during rationale cancels without reopening on resume`() {
        val c = activity()
        vm.onAlertChange(c.get(), true); positive()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        dialog.cancel()
        c.pause().resume()
        vm.validate()
        assertThat(org.robolectric.shadows.ShadowDialog.getLatestDialog()).isSameAs(dialog)
        assertThat(dialog.isShowing).isFalse()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        vm.disposeAlerts(); c.pause().stop().destroy()
    }
    private fun openSettings(c: org.robolectric.android.controller.ActivityController<androidx.appcompat.app.AppCompatActivity>): org.robolectric.shadows.ShadowActivity.IntentForResult {
        vm.onAlertChange(c.get(), true); positive(); positive()
        return shadowOf(c.get()).nextStartedActivityForResult
    }
    @Test fun `return from settings unchanged consumes denial and does not reopen`() {
        val c = activity()
        val request = openSettings(c)
        assertThat(request.intent.action).isEqualTo(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        c.get().activityResultRegistry.dispatchResult(request.requestCode, android.app.Activity.RESULT_CANCELED, null)
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertThat(dialog.isShowing).isFalse()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        c.pause().resume(); vm.validate()
        assertThat(org.robolectric.shadows.ShadowDialog.getLatestDialog()).isSameAs(dialog)
        vm.onAlertChange(c.get(), true)
        assertThat(org.robolectric.shadows.ShadowDialog.getLatestDialog()).isNotSameAs(dialog)
        vm.disposeAlerts(); c.pause().stop().destroy()
    }
    @Test fun `settings grant continues exactly once and mute cancels alarm`() {
        val c = activity(); val request = openSettings(c)
        shadowOf(c.get().application).grantPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        c.get().activityResultRegistry.dispatchResult(request.requestCode, android.app.Activity.RESULT_CANCELED, null)
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
        c.get().activityResultRegistry.dispatchResult(request.requestCode, android.app.Activity.RESULT_CANCELED, null)
        assertThat(alarms.scheduledAlarms).hasSize(1)
        vm.onAlertChange(c.get(), false)
        assertThat(alarms.scheduledAlarms).isEmpty()
        vm.disposeAlerts(); c.pause().stop().destroy()
    }
    @Test fun `settings grant after explicit cancellation cannot activate`() {
        val c = activity(); val request = openSettings(c)
        vm.onAlertChange(c.get(), false)
        shadowOf(c.get().application).grantPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        c.get().activityResultRegistry.dispatchResult(request.requestCode, android.app.Activity.RESULT_CANCELED, null)
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        verify(exactly = 0) { updater.tripSubscription(any()) }
        vm.disposeAlerts(); c.pause().stop().destroy()
    }
    @Test fun `activity recreation invalidates outstanding settings activation`() {
        val c = activity(); val request = openSettings(c)
        c.pause().stop().destroy()
        shadowOf(c.get().application).grantPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        c.get().activityResultRegistry.dispatchResult(request.requestCode, android.app.Activity.RESULT_CANCELED, null)
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        vm.disposeAlerts()
    }
    @Test fun `alarm only trip does not request background location`() {
        val c = activity()
        trip.segmentList.first().geofences = emptyList()
        vm.onAlertChange(c.get(), true); positive()
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
        assertThat(shadowOf(c.get()).nextStartedActivity).isNull()
        vm.onAlertChange(c.get(), false); vm.disposeAlerts(); c.pause().stop().destroy()
    }

    @Test @Config(sdk = [28]) fun `pre Android ten geofences do not request separate background permission`() {
        val c = activity()
        vm.onAlertChange(c.get(), true); positive()
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
        vm.onAlertChange(c.get(), false); vm.disposeAlerts(); c.pause().stop().destroy()
    }
    @Test @Config(sdk = [29]) fun `Android ten background denial remains cancellable`() {
        val c = activity()
        vm.onAlertChange(c.get(), true); positive()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertThat(dialog.isShowing).isTrue()
        dialog.cancel()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        vm.disposeAlerts(); c.pause().stop().destroy()
    }
    @Test fun `trip A B A replaces capability and cancels pending previous trip permission`() {
        val c = activity()
        val segments = TripSegmentsViewModel(context, mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), updater, reminders,
            mockk(relaxed = true), mockk(relaxed = true))
        val setup = TripSegmentsViewModel::class.java.getDeclaredMethod("setupGetOffAlert", TripGroup::class.java, Trip::class.java).apply { isAccessible = true }
        val owner = TripSegmentsViewModel::class.java.getDeclaredField("tripSegmentGetOffAlertsViewModel").apply { isAccessible = true }
        val title = context.getString(com.skedgo.tripkit.ui.R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop)
        remote()
        val a = setup.invoke(segments, TripGroup(), trip) as TripSegmentGetOffAlertsViewModel
        owner.set(segments, a)
        a.onAlertChange(c.get(), true)
        val disclosure = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        val bTrip = com.google.gson.Gson().fromJson(com.google.gson.Gson().toJson(trip), Trip::class.java).apply { tripId = 43; saveURL = "https://example.invalid/trip-b"; subscribeURL = null }
        val b = setup.invoke(segments, TripGroup(), bTrip) as TripSegmentGetOffAlertsViewModel
        owner.set(segments, b)
        assertThat(b.items.single { it.title == title }.isEnabled).isFalse()
        shadowOf(c.get().application).grantPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        positive(disclosure)
        assertThat(alarms.scheduledAlarms).isEmpty()
        verify(exactly = 0) { updater.tripSubscription(any()) }
        val restoredA = setup.invoke(segments, TripGroup(), trip) as TripSegmentGetOffAlertsViewModel
        assertThat(restoredA.items.single { it.title == title }.isEnabled).isTrue()
        assertThat(restoredA.getOffAlertStateOn.value).isFalse()
        restoredA.disposeAlerts(); vm.disposeAlerts(); c.pause().stop().destroy()
    }

    @Test fun `dragging monitoring switch off cancels real alarm and cache`() {
        val c = activity()
        enable()
        val binding = com.skedgo.tripkit.ui.databinding.TripSegmentGetOffAlertBinding.inflate(android.view.LayoutInflater.from(c.get()))
        binding.viewModel = vm
        binding.lifecycleOwner = c.get()
        c.get().setContentView(binding.root)
        binding.executePendingBindings()
        binding.root.measure(android.view.View.MeasureSpec.makeMeasureSpec(1000, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(2000, android.view.View.MeasureSpec.EXACTLY))
        binding.root.layout(0, 0, 1000, 2000)
        val switch = binding.sAlertState
        switch.draw(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(300,200,android.graphics.Bitmap.Config.ARGB_8888)))
        assertThat(switch.isChecked).isTrue()
        val bounds = switch.thumbDrawable.bounds
        val x = bounds.exactCenterX(); val y = bounds.exactCenterY()
        val start = android.os.SystemClock.uptimeMillis()
        listOf(Triple(0L, android.view.MotionEvent.ACTION_DOWN, x),
            Triple(50L, android.view.MotionEvent.ACTION_MOVE, x - 100),
            Triple(100L, android.view.MotionEvent.ACTION_MOVE, x - 200),
            Triple(150L, android.view.MotionEvent.ACTION_UP, x - 250)).forEach { (dt, action, px) ->
            val e = android.view.MotionEvent.obtain(start,start+dt,action,px,y,0)
            switch.dispatchTouchEvent(e); e.recycle()
        }
        assertThat(switch.isChecked).isFalse()
        assertThat(vm.getOffAlertStateOn.value).isFalse()
        assertThat(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
        vm.disposeAlerts(); c.pause().stop().destroy()
    }

    @Test fun `rendering master switch state never replays activation but explicit tap does`() {
        val c = activity()
        val actions = mutableListOf<Boolean>()
        vm.alertStateToggleCustomValidation = { _, checked -> actions.add(checked) }
        val binding = com.skedgo.tripkit.ui.databinding.TripSegmentGetOffAlertBinding.inflate(android.view.LayoutInflater.from(c.get()))
        binding.lifecycleOwner = c.get(); binding.viewModel = vm
        binding.executePendingBindings()
        vm.setGetOffAlertStateOn(true)
        binding.executePendingBindings()
        assertThat(binding.sAlertState.isChecked).isTrue()
        assertThat(actions).isEmpty()
        binding.sAlertState.performClick()
        assertThat(actions).containsExactly(false)
        vm.disposeAlerts(); c.pause().stop().destroy()
    }

    @Test fun `absent URL never registers stale boarding fence but preserves local arrival`() {
        val board = Geofence("board", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"VEHICLE_IS_APPROACHING","Board","Board")
        val arrival = Geofence("arrival", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"TRIP_END","Arrive","Arrive")
        trip.segmentList.first().geofences = listOf(board, arrival)
        enable()
        verify { GeoLocation.createGeoFences(trip, match { it == listOf(arrival) }, any()) }
        verify(exactly = 0) { updater.tripSubscription(any()) }
    }
    @Test fun `present URL preserves boarding fence after successful subscription`() {
        val board = Geofence("board", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"VEHICLE_IS_APPROACHING","Board","Board")
        trip.segmentList.first().geofences = listOf(board)
        remote(); every { updater.tripSubscription(any()) } returns Observable.just(Any())
        enable()
        verify { GeoLocation.createGeoFences(trip, match { it == listOf(board) }, any()) }
    }
    @Test fun `stale boarding only fence without URL does not require background location`() {
        val c = activity()
        trip.segmentList.first().geofences = listOf(Geofence("board", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"VEHICLE_IS_APPROACHING","Board","Board"))
        vm.onAlertChange(c.get(), true); positive()
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
        verify(exactly = 0) { GeoLocation.createGeoFences(any(), match { it.isNotEmpty() }, any()) }
        vm.onAlertChange(c.get(), false); vm.disposeAlerts(); c.pause().stop().destroy()
    }

}
