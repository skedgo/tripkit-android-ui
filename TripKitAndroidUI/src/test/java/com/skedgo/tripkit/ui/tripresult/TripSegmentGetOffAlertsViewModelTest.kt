package com.skedgo.tripkit.ui.tripresult

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.skedgo.TripKit
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.checkIfLocationProviderIsEnabled
import com.skedgo.tripkit.notification.cancelChannelNotifications
import com.skedgo.tripkit.routing.GetOffAlertCache
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.utils.requestPermissionGently
import com.skedgo.tripkit.ui.utils.showConfirmationPopUpDialog
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.reactivex.functions.Consumer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripSegmentGetOffAlertsViewModelTest: MockKTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripSegmentGetOffAlertsViewModel
    private lateinit var trip: Trip
    private lateinit var tripUpdater: TripUpdater
    private lateinit var remindersRepository: RemindersRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        initRx()

        trip = mockk(relaxed = true)
        tripUpdater = mockk(relaxed = true)
        remindersRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

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

        GetOffAlertCache.init(context)

        val mockErrorHandler = mockk<Consumer<Throwable>>(relaxed = true)

        val configs = mockk<Configs> {
            every { hasGetOffAlerts() } returns true
            every { context() } returns context
            every { errorHandler() } returns mockErrorHandler
        }

        TripKit.initialize(configs)

        viewModel = TripSegmentGetOffAlertsViewModel(
            trip = trip,
            defaultValue = false,
            tripUpdater = tripUpdater,
            remindersRepository = remindersRepository
        )
    }


    @After
    fun tearDown() {
        tearDownRx()
        unmockkAll()
    }

    @Test
    fun `validate updates getOffAlertStateOn based on GetOffAlertCache`() {
        // Arrange
        every { GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid()) } returns true
        val observer = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.getOffAlertStateOn.observeForever(observer)

        viewModel.validate()

        verify {
            observer.onChanged(true)
        }
    }

    @Test
    fun `setup updates DiffObservableList items`() {
        // Arrange
        val detailViewModel1 = mockk<TripSegmentGetOffAlertDetailViewModel>(relaxed = true)
        val detailViewModel2 = mockk<TripSegmentGetOffAlertDetailViewModel>(relaxed = true)
        val details = listOf(detailViewModel1, detailViewModel2)

        // Act
        viewModel.setup(context, details)

        // Assert
        assertEquals(2, viewModel.items.size)
        assertTrue(viewModel.items.containsAll(details))
    }

    @Test
    fun `setAlertState turns off alert and triggers unsubscribe trip updater`() {
        // Arrange
        val listener = mockk<(Boolean) -> Unit>(relaxed = true)
        viewModel.alertStateListener = listener

        viewModel.setGetOffAlertStateOn(true)

        every { trip.getTripUuid() } returns "uuid"
        every { trip.group?.uuid() } returns "groupUuid"

        trip.unsubscribeURL = "unsubscribe-url"

        every { tripUpdater.tripSubscription(any()) } returns mockk {
            every { subscribe(any(), any()) } returns mockk()
        }

        // Act
        viewModel.setAlertState(context, isOn = false)

        // Assert
        assertTrue(viewModel.getOffAlertStateOn.value == false)
        verify { listener.invoke(false) }
        //verify { tripUpdater.tripSubscription("unsubscribe-url") }
    }
}
