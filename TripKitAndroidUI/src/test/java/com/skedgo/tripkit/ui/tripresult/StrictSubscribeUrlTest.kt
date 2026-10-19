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
class StrictSubscribeUrlTest : MockKTest() {
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

    private val boardingTitle get() = context.getString(com.skedgo.tripkit.ui.R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop)
    private fun enable() {
        TripSegmentGetOffAlertsViewModel::class.java.getDeclaredMethod("enableAlerts", Context::class.java)
            .apply { isAccessible = true }.invoke(vm, context)
    }
    private fun segments() = TripSegmentsViewModel(context, mockk(relaxed = true), mockk(relaxed = true),
        mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
        mockk(relaxed = true), mockk(relaxed = true), updater, reminders,
        mockk(relaxed = true), mockk(relaxed = true))
    private fun details(segments: TripSegmentsViewModel = segments(), selected: Trip = trip): TripSegmentGetOffAlertsViewModel {
        val method = TripSegmentsViewModel::class.java.getDeclaredMethod("setupGetOffAlert", TripGroup::class.java, Trip::class.java).apply { isAccessible = true }
        val result = method.invoke(segments, TripGroup(), selected) as TripSegmentGetOffAlertsViewModel
        TripSegmentsViewModel::class.java.getDeclaredField("tripSegmentGetOffAlertsViewModel").apply { isAccessible = true }.set(segments, result)
        return result
    }
    private fun row(owner: TripSegmentGetOffAlertsViewModel) = owner.items.single { it.title == boardingTitle }
    private fun checkBinding(detail: TripSegmentGetOffAlertDetailViewModel, expected: Boolean) {
        val controller = org.robolectric.Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java)
        controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat); controller.setup()
        val binding = com.skedgo.tripkit.ui.databinding.ItemAlertDetailBinding.inflate(android.view.LayoutInflater.from(controller.get()))
        controller.get().setContentView(binding.root)
        // Reuse the same real binding in both directions, as a recycled row would be reused.
        for (value in listOf(!expected, expected, !expected, expected)) {
            binding.viewModel = if (value == expected) detail else TripSegmentGetOffAlertDetailViewModel(detail.icon, detail.title, value)
            binding.executePendingBindings()
            val color = androidx.core.content.ContextCompat.getColor(controller.get(), if (value) com.skedgo.tripkit.ui.R.color.black else com.skedgo.tripkit.ui.R.color.disabled)
            assertThat(binding.root.isEnabled).isEqualTo(value)
            assertThat(binding.tvTitle.isEnabled).isEqualTo(value)
            assertThat(binding.ivIcon.isEnabled).isEqualTo(value)
            assertThat(binding.tvTitle.currentTextColor).isEqualTo(color)
            assertThat(binding.tvTitle.textColors.getColorForState(binding.tvTitle.drawableState, 0)).isEqualTo(color)
            assertThat(shadowOf(binding.ivIcon.colorFilter as android.graphics.PorterDuffColorFilter).color).isEqualTo(color)
            assertThat(binding.tvTitle.createAccessibilityNodeInfo().isEnabled).isEqualTo(value)
            assertThat(binding.root.hasOnClickListeners()).isFalse()
            assertThat(binding.root.visibility).isEqualTo(android.view.View.VISIBLE)
        }
        controller.pause().stop().destroy()
    }
    private fun invalid(value: String?) {
        trip.subscribeURL = value
        val owner = details()
        assertThat(row(owner).isEnabled).describedAs("subscribeURL=%s", value).isFalse()
        assertThat(owner.items.first().isEnabled).isTrue()
        checkBinding(row(owner), false)
        owner.disposeAlerts()
    }
    @Test fun `missing URL binds disabled grey`() = invalid(null)
    @Test fun `empty URL binds disabled grey`() = invalid("")
    @Test fun `whitespace URL binds disabled grey`() = invalid("   ")
    @Test fun `literal null URL binds disabled grey`() = invalid("null")
    @Test fun `non URL URL binds disabled grey`() = invalid("not-a-url")
    @Test fun `relative URL binds disabled grey`() = invalid("/subscribe/123")
    @Test fun `javascript URL binds disabled grey`() = invalid("javascript:alert(1)")
    @Test fun `missing host URL binds disabled grey`() = invalid("https://")
    @Test fun `file URL binds disabled grey`() = invalid("file:///tmp/subscribe")
    @Test fun `content URL binds disabled grey`() = invalid("content://trip/123")
    @Test fun `ftp URL binds disabled grey`() = invalid("ftp://valid.example/path")
    @Test fun `invalid hostname URL binds disabled grey`() = invalid("https://bad host/path")
    @Test fun `invalid port URL binds disabled grey`() = invalid("https://valid.example:99999/path")
    @Test fun `scheme relative URL binds disabled grey`() = invalid("//valid.example/path")
    @Test fun `missing authority URL binds disabled grey`() = invalid("https:valid.example/path")
    @Test @Config(qualifiers = "night") fun `invalid and absent URL bind disabled colors in dark theme`() {
        invalid(null); invalid("not-a-url")
    }
    @Test fun `valid absolute URLs retain eligible real binding`() {
        for (url in listOf("https://valid.example/path", "http://valid.example/path", "  HTTPS://valid.example:8443/a%20b?key=x%2Fy&v=1  ", "https://[::1]:8443/path")) {
            trip.subscribeURL = url
            val owner = details(); assertThat(row(owner).isEnabled).isTrue(); checkBinding(row(owner), true); owner.disposeAlerts()
        }
    }
    @Test @Config(qualifiers = "night") fun `valid URL binds primary in dark theme`() {
        trip.subscribeURL = "https://valid.example/path"; checkBinding(row(details()), true)
    }
    @Test fun `invalid URL skips server and retains real local alarm and fences`() {
        trip.subscribeURL = "not-a-url"
        every { configs.userTokenProvider() } returns Callable { "test-token" }
        every { updater.tripSubscription(any()) } returns Observable.just(Unit)
        val board = Geofence("board", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"VEHICLE_IS_APPROACHING","Board","Board")
        val local = Geofence("arrival", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"TRIP_END","Arrive","Arrive")
        trip.segmentList.first().geofences = listOf(board, local)
        enable()
        verify(exactly = 0) { updater.tripSubscription(any()) }
        assertThat(vm.getOffAlertStateOn.value).isTrue()
        assertThat(alarms.scheduledAlarms).hasSize(1)
        verify { GeoLocation.createGeoFences(trip, match { it == listOf(local) }, any()) }
    }
    @Test fun `valid invalid valid refresh updates recycled binding and restored owner`() {
        val segments = segments()
        for (url in listOf("https://valid.example/path", "not-a-url", "https://valid.example/path")) {
            trip.subscribeURL = url
            val owner = details(segments)
            val valid = url.startsWith("https://")
            assertThat(row(owner).isEnabled).isEqualTo(valid); checkBinding(row(owner), valid)
        }
        trip.subscribeURL = "null"
        val reopened = details(segments())
        assertThat(row(reopened).isEnabled).isFalse(); checkBinding(row(reopened), false)
    }
    @Test fun `trip A valid B invalid A has no binding leakage`() {
        val segments = segments()
        trip.subscribeURL = "https://valid.example/path"
        val b = com.google.gson.Gson().fromJson(com.google.gson.Gson().toJson(trip), Trip::class.java).apply {
            tripId = 43; saveURL = "https://valid.example/trip-b"; subscribeURL = "/subscribe/123"
        }
        for (selected in listOf(trip,b,trip)) {
            val owner = details(segments, selected); val valid = selected === trip
            assertThat(row(owner).isEnabled).isEqualTo(valid); checkBinding(row(owner), valid)
        }
    }
    @Test fun `valid to invalid active refresh removes stale boarding fence and keeps local alerts`() {
        val segments = segments()
        trip.subscribeURL = "https://valid.example/path"
        every { configs.userTokenProvider() } returns Callable { "test-token" }
        every { updater.tripSubscription(any()) } returns Observable.just(Unit)
        val board = Geofence("board", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"VEHICLE_IS_APPROACHING","Board","Board")
        val local = Geofence("arrival", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"TRIP_END","Arrive","Arrive")
        trip.segmentList.first().geofences = listOf(board,local)
        val owner = details(segments)
        TripSegmentGetOffAlertsViewModel::class.java.getDeclaredMethod("enableAlerts", Context::class.java).apply { isAccessible = true }.invoke(owner,context)
        clearMocks(GeoLocation, answers = false)
        trip.subscribeURL = "not-a-url"
        val refreshed = details(segments)
        assertThat(refreshed).isSameAs(owner)
        verify { GeoLocation.clearGeofences() }
        verify { GeoLocation.createGeoFences(trip, match { it == listOf(local) }, any()) }
        assertThat(owner.getOffAlertStateOn.value).isTrue()
        val saved = shadowOf(alarms.scheduledAlarms.single().operation).savedIntent
        assertThat(saved.getStringExtra(TripAlarmBroadcastReceiver.EXTRA_START_TRIP_EVENT_TRIP)).contains("not-a-url")
        owner.onAlertChange(context,false)
    }
    @Test fun `late server acknowledgement cannot activate refreshed invalid trip`() {
        val segments = segments()
        trip.subscribeURL = "https://valid.example/path"
        every { configs.userTokenProvider() } returns Callable { "test-token" }
        val response = PublishSubject.create<Any>()
        every { updater.tripSubscription(any()) } returns response
        val owner = details(segments)
        TripSegmentGetOffAlertsViewModel::class.java.getDeclaredMethod("enableAlerts", Context::class.java).apply { isAccessible = true }.invoke(owner,context)
        trip.subscribeURL = "not-a-url"; details(segments)
        response.onNext(Unit)
        assertThat(owner.getOffAlertStateOn.value).isFalse()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }
    @Test fun `post subscription trip refresh updates visible boarding row`() {
        trip.subscribeURL = "https://valid.example/path"
        trip.updateURL = "https://valid.example/update"
        every { configs.userTokenProvider() } returns Callable { "test-token" }
        every { updater.tripSubscription(any()) } returns Observable.just(Unit)
        val updated = com.google.gson.Gson().fromJson(com.google.gson.Gson().toJson(trip), Trip::class.java).apply { subscribeURL = "not-a-url" }
        every { updater.getUpdateAsync(any<String>()) } returns Observable.just(updated)
        val owner = details()
        TripSegmentGetOffAlertsViewModel::class.java.getDeclaredMethod("enableAlerts", Context::class.java).apply { isAccessible = true }.invoke(owner,context)
        assertThat(row(owner).isEnabled).isFalse(); checkBinding(row(owner), false)
        assertThat(owner.getOffAlertStateOn.value).isTrue()
        owner.onAlertChange(context,false)
    }
    @Test fun `reopened active trip with invalid URL removes previously valid boarding fences`() {
        trip.subscribeURL = "https://valid.example/path"
        every { configs.userTokenProvider() } returns Callable { "test-token" }
        every { updater.tripSubscription(any()) } returns Observable.just(Unit)
        val board = Geofence("board", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"VEHICLE_IS_APPROACHING","Board","Board")
        val local = Geofence("arrival", "circle", "ENTER", Coordinate(-35.28,149.13),100.0,"TRIP_END","Arrive","Arrive")
        trip.segmentList.first().geofences = listOf(board,local)
        enable(); vm.disposeAlerts()
        clearMocks(GeoLocation, answers = false)
        trip.subscribeURL = "not-a-url"
        val reopened = details()
        assertThat(reopened.getOffAlertStateOn.value).isTrue()
        verify { GeoLocation.clearGeofences() }
        verify { GeoLocation.createGeoFences(trip, match { it == listOf(local) }, any()) }
        assertThat(row(reopened).isEnabled).isFalse(); checkBinding(row(reopened),false)
        assertThat(alarms.scheduledAlarms).hasSize(1)
        reopened.onAlertChange(context,false)
    }
}
