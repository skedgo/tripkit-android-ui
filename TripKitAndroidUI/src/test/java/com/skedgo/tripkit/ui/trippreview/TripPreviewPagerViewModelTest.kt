package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import io.mockk.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import timber.log.Timber
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class TripPreviewPagerViewModelTest : MockKTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripPreviewPagerViewModel

    private val tripGroupRepository: TripGroupRepository = mockk(relaxed = true)
    private val printTime: PrintTime = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val getTransportIconTintStrategy: GetTransportIconTintStrategy = mockk(relaxed = true)

    @Before
    fun setUp() {
        initRx()
        mockkStatic(Timber::class)

        every { Timber.e(any<Throwable>()) } just Runs
        viewModel = TripPreviewPagerViewModel(tripGroupRepository, printTime)
    }

    @After
    fun tearDown() {
        tearDownRx()
        unmockkAll()
    }

    @Test
    fun `loadTripGroup should post trip group to LiveData`() {
        val tripGroupId = "test-trip-group-id"
        val tripGroup = mockk<TripGroup>(relaxed = true)

        every {
            tripGroupRepository.getTripGroup(tripGroupId)
        } returns Observable.just(tripGroup)

        val observer = mockk<Observer<TripGroup>>(relaxed = true)
        viewModel.tripGroup.observeForever(observer)

        viewModel.loadTripGroup(tripGroupId)

        verify { observer.onChanged(tripGroup) }
    }

    @Test
    fun `startUpdateTripPolling should fetch updated trip group`() {
        val tripGroupId = "test-trip-group-id"
        val tripGroup = mockk<TripGroup>(relaxed = true)
        val testScheduler = TestScheduler()
        every {
            tripGroupRepository.getTripGroup(tripGroupId)
        } returns Observable.just(tripGroup)

        val observer = mockk<Observer<TripGroup>>(relaxed = true)
        viewModel.tripGroupFromPolling.observeForever(observer)

        viewModel.startUpdateTripPolling(tripGroupId, testScheduler)

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)

        verify { observer.onChanged(tripGroup) }
    }

    @Test
    fun `updateTrip should call repository updateTrip`() {
        val tripGroupId = "tripGroupId"
        val oldTripUuid = "oldTripUuid"
        val trip = mockk<Trip>(relaxed = true)

        every {
            tripGroupRepository.updateTrip(tripGroupId, oldTripUuid, trip)
        } returns Completable.complete()

        viewModel.updateTrip(tripGroupId, oldTripUuid, trip)

        verify {
            tripGroupRepository.updateTrip(tripGroupId, oldTripUuid, trip)
        }
    }
}
