package com.skedgo.tripkit.ui.timetables

import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import io.mockk.mockk
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.inject.Provider

class TimetableDepartedRealtimeFilterTest : MockKTest() {

    @Before
    fun setUp() {
        initRx()
    }

    @After
    fun tearDown() {
        tearDownRx()
    }

    @Test
    fun `current time mode does not create visible ServiceViewModel for negative countdown`() {
        val service = timetableEntry(serviceTripId = "past", departureSecs = NOW_SECS - 60)

        val visible = createVisibleServiceViewModels(listOf(service), isSelectedTime = false)

        assertEquals(emptyList<String>(), visible.serviceTripIds())
    }

    @Test
    fun `current time mode keeps visible ServiceViewModel for zero countdown`() {
        val service = timetableEntry(serviceTripId = "now", departureSecs = NOW_SECS)

        val visible = createVisibleServiceViewModels(listOf(service), isSelectedTime = false)

        assertEquals(listOf("now"), visible.serviceTripIds())
    }

    @Test
    fun `current time mode keeps visible ServiceViewModel for positive countdown`() {
        val service = timetableEntry(serviceTripId = "future", departureSecs = NOW_SECS + 60)

        val visible = createVisibleServiceViewModels(listOf(service), isSelectedTime = false)

        assertEquals(listOf("future"), visible.serviceTripIds())
    }

    @Test
    fun `current time mode does not create visible ServiceViewModel for cancelled service`() {
        val service = timetableEntry(
            serviceTripId = "cancelled",
            departureSecs = NOW_SECS + 60,
            isCancelled = true
        )

        val visible = createVisibleServiceViewModels(listOf(service), isSelectedTime = false)

        assertEquals(emptyList<String>(), visible.serviceTripIds())
    }

    @Test
    fun `selected time mode keeps negative countdown ServiceViewModel visible`() {
        val service = timetableEntry(serviceTripId = "past", departureSecs = NOW_SECS - 60)

        val visible = createVisibleServiceViewModels(listOf(service), isSelectedTime = true)

        assertEquals(listOf("past"), visible.serviceTripIds())
    }

    @Test
    fun `current time mode removes past and cancelled items while preserving remaining order`() {
        val past = timetableEntry(serviceTripId = "past", departureSecs = NOW_SECS - 60)
        val cancelled = timetableEntry(
            serviceTripId = "cancelled",
            departureSecs = NOW_SECS + 30,
            isCancelled = true
        )
        val now = timetableEntry(serviceTripId = "now", departureSecs = NOW_SECS)
        val future = timetableEntry(serviceTripId = "future", departureSecs = NOW_SECS + 60)

        val visible = createVisibleServiceViewModels(
            listOf(past, cancelled, now, future),
            isSelectedTime = false
        )

        assertEquals(listOf("now", "future"), visible.serviceTripIds())
    }

    @Test
    fun `selected time mode keeps same rows that previous behavior mapped`() {
        val past = timetableEntry(serviceTripId = "past", departureSecs = NOW_SECS - 60)
        val cancelled = timetableEntry(
            serviceTripId = "cancelled",
            departureSecs = NOW_SECS + 30,
            isCancelled = true
        )
        val now = timetableEntry(serviceTripId = "now", departureSecs = NOW_SECS)
        val future = timetableEntry(serviceTripId = "future", departureSecs = NOW_SECS + 60)

        val visible = createVisibleServiceViewModels(
            listOf(past, cancelled, now, future),
            isSelectedTime = true
        )

        assertEquals(listOf("past", "cancelled", "now", "future"), visible.serviceTripIds())
    }

    @Test
    fun `current time mode uses realtime vehicle departure for visible ServiceViewModel filtering`() {
        val service = timetableEntry(
            serviceTripId = "vehicle-past",
            departureSecs = NOW_SECS + 600,
            vehicleDepartureSecs = NOW_SECS - 60
        )

        val visible = createVisibleServiceViewModels(listOf(service), isSelectedTime = false)

        assertEquals(emptyList<String>(), visible.serviceTripIds())
    }

    private fun createVisibleServiceViewModels(
        services: List<TimetableEntry>,
        isSelectedTime: Boolean
    ): List<ServiceViewModel> {
        return createVisibleServiceViewModels(
            services = services,
            currentTripId = "",
            timeZone = DateTimeZone.UTC,
            isSelectedTime = isSelectedTime,
            nowInMillis = TimeUnit.SECONDS.toMillis(NOW_SECS),
            serviceViewModelProvider = Provider { FakeServiceViewModel() },
            timetableEntryChosen = PublishRelay.create()
        )
    }

    private fun List<ServiceViewModel>.serviceTripIds(): List<String?> {
        return map { it.service.serviceTripId }
    }

    private fun timetableEntry(
        serviceTripId: String,
        departureSecs: Long,
        vehicleDepartureSecs: Long? = null,
        isCancelled: Boolean = false
    ): TimetableEntry {
        return TimetableEntry().apply {
            this.serviceTripId = serviceTripId
            this.startTimeInSecs = departureSecs
            this.realTimeDeparture = -1
            this.isCancelled = isCancelled
            this.realtimeVehicle = vehicleDepartureSecs?.let {
                RealTimeVehicle().apply {
                    arriveAtStartStopTime = it
                    this.serviceTripId = serviceTripId
                }
            }
        }
    }

    private class FakeServiceViewModel : ServiceViewModel() {
        override val occupancyViewModel: OccupancyViewModel = mockk(relaxed = true)
        override val serviceAlertViewModel: ServiceAlertViewModel = mockk(relaxed = true)
        override val serviceNumber = MutableLiveData<String>()
        override val secondaryText = MutableLiveData<String>()
        override val secondaryTextColor = MutableLiveData<Int>()
        override val tertiaryText = MutableLiveData<String>()
        override val quaternaryText = MutableLiveData<String>()
        override val countDownTimeText = MutableLiveData<String>()
        override val countDownTimeTextColor = MutableLiveData<Int>()
        override val alpha = MutableLiveData<Float>()
        override val countDownTimeTextBack = MutableLiveData<Drawable>()
        override val serviceColor = MutableLiveData<Int>()
        override val showOccupancyInfo = MutableLiveData<Boolean>()
        override val showBicycleAccessible = MutableLiveData<Boolean>()
        override val isCurrentTrip = MutableLiveData<Boolean>()
        override val wheelchairIcon = MutableLiveData<Drawable?>()
        override val wheelchairTint = MutableLiveData<Int>()
        override val wheelchairBackgroundTint = MutableLiveData<Drawable>()
        override val modeInfo = MutableLiveData<ModeInfo>()
        override val onItemClick = TapAction.create { service }
        override val onAlertsClick = TapAction.create<List<RealtimeAlert>?> { null }
        override lateinit var service: TimetableEntry
        override lateinit var dateTimeZone: DateTimeZone

        override fun getRealTimeDeparture(): Long {
            return realTimeDeparture(service, service.realtimeVehicle)
        }

        override fun setService(
            _currentTripId: String,
            _service: TimetableEntry,
            _dateTimeZone: DateTimeZone
        ) {
            service = _service
            dateTimeZone = _dateTimeZone
        }
    }

    private companion object {
        const val NOW_SECS = 1_700_000_000L
    }
}
