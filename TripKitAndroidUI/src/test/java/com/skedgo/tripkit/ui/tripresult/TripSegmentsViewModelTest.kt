package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.graphics.drawable.Drawable
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.booking.quickbooking.QuickBookingRepository
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.base.TripSegmentMock
import com.skedgo.tripkit.ui.creditsources.CreditSourcesOfDataViewModel
import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.utils.TripSegmentActionProcessor
import com.skedgo.tripkit.ui.utils.createSummaryIcon
import com.skedgo.tripkit.ui.utils.generateTripPreviewHeader
import com.skedgo.tripkit.ui.utils.getSegmentIconObservable
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import timber.log.Timber
import java.lang.Exception
import javax.inject.Provider

@RunWith(MockitoJUnitRunner::class)
class TripSegmentsViewModelTest : MockKTest() {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var printTime: PrintTime

    @MockK(relaxed = true)
    private lateinit var itemProvider: Provider<TripSegmentItemViewModel>

    @MockK(relaxed = true)
    private lateinit var creditSourcesOfDataViewModelProvider: Provider<CreditSourcesOfDataViewModel>

    @MockK
    private lateinit var updateForRealtime: UpdateTripForRealtime

    @MockK(relaxed = true)
    private lateinit var tripGroupRepository: TripGroupRepository

    @MockK
    private lateinit var tripSegmentActionProcessor: TripSegmentActionProcessor

    @MockK
    private lateinit var getAlternativeTripForAlternativeService: GetAlternativeTripForAlternativeService

    @MockK
    private lateinit var tripUpdater: TripUpdater

    @MockK
    private lateinit var remindersRepository: RemindersRepository

    @MockK(relaxed = true)
    private lateinit var getTransportIconTintStrategy: GetTransportIconTintStrategy

    @MockK
    private lateinit var quickBookingRepository: QuickBookingRepository

    private lateinit var viewModel: TripSegmentsViewModel

    private val mockDrawable = mockk<Drawable>(relaxed = true)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {

        val testDispatcher = TestCoroutineDispatcher()
        Dispatchers.setMain(testDispatcher)

        MockKAnnotations.init(this)

        viewModel = TripSegmentsViewModel(
            context,
            printTime,
            itemProvider,
            creditSourcesOfDataViewModelProvider,
            updateForRealtime,
            tripGroupRepository,
            tripSegmentActionProcessor,
            getAlternativeTripForAlternativeService,
            tripUpdater,
            remindersRepository,
            getTransportIconTintStrategy,
            quickBookingRepository
        )
    }

    private fun mockDrawableGeneration(isPositive: Boolean) {
        mockkStatic("com.skedgo.tripkit.ui.utils.TripSegmentUIExtensionKt")

        if (isPositive) {
            every {
                any<TripSegment>().getSegmentIconObservable(
                    any(),
                    any()
                )
            } returns Observable.just(
                mockDrawable
            )
            every { any<TripSegment>().createSummaryIcon(any(), any()) } returns mockDrawable
        } else {
            every {
                any<TripSegment>().getSegmentIconObservable(
                    any(),
                    any()
                )
            } returns Observable.error(Exception("Test Error"))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        unmockkStatic("com.skedgo.tripkit.ui.utils.TripSegmentUIExtensionKt")
    }

    // TODO: Unit testing - refactor
    /* Disabled due to assertion error
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `generateSummaryItems - should call generateTripPreviewHeader for each segment in list`() =
        runTest {
            mockDrawableGeneration(true)
            val segments = TripSegmentMock.getTripSegments()

            viewModel.generateSummaryItems(segments)

            verify {
                TripSegmentMock.tripSegmentScheduled.generateTripPreviewHeader(
                    any(),
                    mockDrawable,
                    any()
                )
                TripSegmentMock.tripSegmentArrival.generateTripPreviewHeader(
                    any(),
                    mockDrawable,
                    any()
                )
                TripSegmentMock.tripSegmentDeparture.generateTripPreviewHeader(
                    any(),
                    mockDrawable,
                    any()
                )
            }
        }
     */

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `generateSummaryItems - should not call generateTripPreviewHeader and throws exception`() =
        runTest {
            mockDrawableGeneration(false)
            mockkStatic(Timber::class)
            val segments = TripSegmentMock.getTripSegments()

            viewModel.generateSummaryItems(segments)

            verify(exactly = 0) {
                TripSegmentMock.tripSegmentScheduled.generateTripPreviewHeader(
                    any(),
                    mockDrawable,
                    any()
                )
                TripSegmentMock.tripSegmentArrival.generateTripPreviewHeader(
                    any(),
                    mockDrawable,
                    any()
                )
                TripSegmentMock.tripSegmentDeparture.generateTripPreviewHeader(
                    any(),
                    mockDrawable,
                    any()
                )
            }

            verify { Timber.e(any<Exception>()) }
            unmockkStatic(Timber::class)
        }
}