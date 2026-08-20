package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.skedgo.TripKit
import com.skedgo.tripkit.ServiceResponse
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.ServiceColor
import com.skedgo.tripkit.servicedetail.ServiceDetailRepository
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.GetRealtimeText
import com.skedgo.tripkit.ui.timetables.GetServiceTertiaryText
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import io.mockk.*
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.joda.time.DateTimeZone
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceDetailViewModelTest: MockKTest() {

    private lateinit var viewModel: ServiceDetailViewModel

    private val context: Context = mockk(relaxed = true)
    private val tripKit: TripKit = mockk(relaxed = true)
    private val regionService: RegionService = mockk()
    private val serviceDetailRepository: ServiceDetailRepository = mockk()
    private val occupancyViewModel: OccupancyViewModel = mockk(relaxed = true)
    private val serviceViewModelProvider: Provider<ServiceDetailItemViewModel> = mockk()
    private val getServiceTertiaryText: GetServiceTertiaryText = mockk()
    private val getRealtimeText: GetRealtimeText = mockk()

    private val stop: ScheduledStop = mockk()
    private val entry: TimetableEntry = mockk()
    private val modeInfo: ModeInfo = mockk()
    private val serviceColor: ServiceColor = mockk()
    private val realTimeVehicle: RealTimeVehicle = mockk()
    private val drawable: Drawable = mockk()

    @Before
    fun setup() {
        initRx()
        mockkObject(TripKit.Companion)
        every { TripKit.getInstance() } returns tripKit

        every { context.getString(R.string.wheelchair_accessible) } returns "Wheelchair Accessible"
        every { context.getString(R.string.not_wheelchair_accessible) } returns "Not Wheelchair Accessible"
        every { ContextCompat.getDrawable(context, R.drawable.ic_wheelchair) } returns drawable
        every { ContextCompat.getDrawable(context, R.drawable.ic_wheelchair_not_accessible) } returns drawable
        every {
            serviceDetailRepository.getService(any<String>(), any<String>(), any(), any(), any(), any(), any())
        } returns Observable.just(mockk(relaxed = true))
        every {
            serviceDetailRepository.getService(any<List<String>>(), any(), any(), any(), any(), any(), any(), any())
        } returns Observable.just(mockk(relaxed = true))

        viewModel = ServiceDetailViewModel(
            context,
            regionService,
            serviceDetailRepository,
            occupancyViewModel,
            serviceViewModelProvider,
            getServiceTertiaryText,
            getRealtimeText
        )
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `setup should set values correctly`() {
        every { serviceColor.color } returns Color.BLUE

        viewModel.setup(
            region = "Region1",
            serviceId = "Service1",
            serviceName = "Bus Service",
            serviceNumber = "123",
            serviceColor = serviceColor,
            operator = "Operator",
            startStopCode = "StartStop",
            endStopCode = "EndStop",
            embarkation = 123456789,
            realTimeVehicle = null,
            wheelchairAccessible = true,
            bicycleAccessible = false
        )

        assertEquals("Bus Service", viewModel.stationName.get())
        assertEquals("123", viewModel.serviceNumber.get())
        assertEquals(Color.BLUE, viewModel.serviceColor.get())
    }

    @Test
    fun `setup from timetable should request complete service and use region timezone`() {
        val region: Region = mockk()
        val regionUrls = arrayListOf("https://api.example.com/")
        val timetableEntry = TimetableEntry().apply {
            serviceTripId = "service-trip-id"
            serviceName = "Haymarket Bus Station"
            serviceDirection = "Haymarket Bus Station"
            serviceNumber = "44"
            operator = "First Leicester"
            startStopCode = "lecdpmtw"
            startTimeInSecs = 1_786_551_900
        }

        every { regionService.getRegionByLocationAsync(stop) } returns Observable.just(region)
        every { region.name } returns "GB_ENG_Leicester"
        every { region.getURLs() } returns regionUrls
        every { region.timezone } returns "UTC"
        every { stop.timeZone } returns "+08:00"
        every {
            getRealtimeText.execute(
                DateTimeZone.UTC,
                timetableEntry,
                null
            )
        } returns Pair("Scheduled", R.color.black1)
        every {
            serviceDetailRepository.getService(
                baseUrls = regionUrls,
                region = "GB_ENG_Leicester",
                serviceTripId = "service-trip-id",
                operator = "First Leicester",
                startStopCode = null,
                endStopCode = null,
                embarkationTimeInSecs = 1_786_551_900,
                encode = true
            )
        } returns Observable.just(mockk(relaxed = true))

        viewModel.setup(stop, timetableEntry)

        verify(exactly = 1) {
            getRealtimeText.execute(
                DateTimeZone.UTC,
                timetableEntry,
                null
            )
        }
        verify(exactly = 1) {
            serviceDetailRepository.getService(
                baseUrls = regionUrls,
                region = "GB_ENG_Leicester",
                serviceTripId = "service-trip-id",
                operator = "First Leicester",
                startStopCode = null,
                endStopCode = null,
                embarkationTimeInSecs = 1_786_551_900,
                encode = true
            )
        }
    }

    @Test
    fun `processResponse should update items`() {
        val response: ServiceResponse = mockk()
        val serviceItemViewModel: ServiceDetailItemViewModel = mockk(relaxed = true)

        every { response.shapes() } returns listOf(mockk {
            every { stops } returns listOf(mockk {
                every { name } returns "Stop1"
            })
            every { serviceColor.color } returns Color.GREEN
            every { isTravelled } returns false
        })
        every { serviceViewModelProvider.get() } returns serviceItemViewModel

        viewModel.processResponse(response)

        assertNotNull(viewModel.items.get())
        assertFalse(viewModel.items.get()!!.isEmpty())
        verify { serviceItemViewModel.setStop(context, any(), Color.GREEN, true) }
        verify { serviceItemViewModel.setDrawable(context, ServiceDetailItemViewModel.LineDirection.MIDDLE) }
    }

    @Test
    fun `processResponse should mark stops before timetable stop as travelled`() {
        val response: ServiceResponse = mockk()
        val passedStop: ServiceStop = mockk {
            every { code } returns "passed"
        }
        val selectedStop: ServiceStop = mockk {
            every { code } returns "selected"
        }
        val upcomingStop: ServiceStop = mockk {
            every { code } returns "upcoming"
        }
        val passedItem: ServiceDetailItemViewModel = mockk(relaxed = true)
        val selectedItem: ServiceDetailItemViewModel = mockk(relaxed = true)
        val upcomingItem: ServiceDetailItemViewModel = mockk(relaxed = true)

        every { response.shapes() } returns listOf(mockk {
            every { stops } returns listOf(passedStop, selectedStop, upcomingStop)
            every { serviceColor.color } returns Color.GREEN
            every { isTravelled } returns true
        })
        every { serviceViewModelProvider.get() } returnsMany listOf(
            passedItem,
            selectedItem,
            upcomingItem
        )

        viewModel.processResponse(response, travelledBoundaryStopCode = "selected")

        verify { passedItem.setStop(context, passedStop, Color.GREEN, true) }
        verify { selectedItem.setStop(context, selectedStop, Color.GREEN, false) }
        verify { upcomingItem.setStop(context, upcomingStop, Color.GREEN, false) }
    }
}
