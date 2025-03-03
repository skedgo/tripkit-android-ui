package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.skedgo.TripKit
import com.skedgo.tripkit.ServiceApi
import com.skedgo.tripkit.ServiceResponse
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.ServiceColor
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.GetRealtimeText
import com.skedgo.tripkit.ui.timetables.GetServiceTertiaryText
import com.skedgo.tripkit.ui.timetables.GetServiceTitleText
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import io.mockk.*
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
    private val serviceApi: ServiceApi = mockk()
    private val occupancyViewModel: OccupancyViewModel = mockk(relaxed = true)
    private val serviceViewModelProvider: Provider<ServiceDetailItemViewModel> = mockk()
    private val serviceAlertViewModel: ServiceAlertViewModel = mockk()
    private val loadServices: LoadServices = mockk()
    private val getServiceTitleText: GetServiceTitleText = mockk()
    private val getServiceTertiaryText: GetServiceTertiaryText = mockk()
    private val getRealtimeText: GetRealtimeText = mockk()
    private val errorLogger: ErrorLogger = mockk()

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
        every { serviceApi.getServiceAsync(any(), any(), any(), any(), any(), any(), any()) } returns Observable.just(mockk())

        viewModel = ServiceDetailViewModel(
            context,
            regionService,
            serviceApi,
            occupancyViewModel,
            serviceViewModelProvider,
            serviceAlertViewModel,
            loadServices,
            getServiceTitleText,
            getServiceTertiaryText,
            getRealtimeText,
            errorLogger
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
}
