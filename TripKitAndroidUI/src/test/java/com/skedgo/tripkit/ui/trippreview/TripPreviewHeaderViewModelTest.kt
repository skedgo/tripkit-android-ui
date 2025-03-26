package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummary
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummaryItemViewModel
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentsSummaryData
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.amshove.kluent.internal.assertEquals
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripPreviewHeaderViewModelTest: MockKTest() {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripPreviewHeaderViewModel
    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun setup() {
        initDispatchers()

        viewModel = TripPreviewHeaderViewModel()

        context = mockk(relaxed = true)
        resources = mockk(relaxed = true)

        every { context.resources } returns resources
        every { resources.getBoolean(any()) } returns false
    }

    @After
    fun tearDown() {
        tearDownDispatchers()
        unmockkAll()
    }

    @Test
    fun `setup should populate items and update LiveData`() {
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)

        every { context.resources } returns resources
        every { resources.getBoolean(R.bool.is_right_to_left) } returns false

        val summary1 = TripSegmentSummary(
            title ="Segment 1",
            subTitle =  "Subtitle 1",
            id = 1L,
            description = "Description 1",
            modeId = "mode1"
        )

        val summary2 = TripSegmentSummary(
            title ="Segment 2",
            subTitle =  "Subtitle 2",
            id = 1L,
            description = "Description 2",
            modeId = "mode1"
        )

        val quickBookingSegment = mockk<TripSegment>(relaxed = true)
        val data = TripSegmentsSummaryData(
            tripSegmentSummaryList = listOf(summary1, summary2),
            quickBookingSegment = quickBookingSegment
        )

        //mockkStatic(TripSegmentSummaryItemViewModel.Companion::parseFromTripSegmentSummary)
        val vm1 = TripSegmentSummaryItemViewModel.parseFromTripSegmentSummary(summary1, false)
        val vm2 = TripSegmentSummaryItemViewModel.parseFromTripSegmentSummary(summary2, false)

        val viewModel = TripPreviewHeaderViewModel()

        viewModel.setup(context, data)

        assertEquals(2, viewModel.items.size)
        with(viewModel.items[0]) {
            assertEquals(vm1.title.value, title.value)
            assertEquals(vm1.subTitle.value, subTitle.value)
            assertEquals(vm1.description.value, description.value)
        }
        with(viewModel.items[1]) {
            assertEquals(vm2.title.value, title.value)
            assertEquals(vm2.subTitle.value, subTitle.value)
            assertEquals(vm2.description.value, description.value)
        }

        assertEquals(quickBookingSegment, viewModel.quickBookingSegment.value)
        assertEquals("Description 1", viewModel.description.value)
        assertEquals(true, viewModel.showDescription.value)
    }

    @Test
    fun `setHideExactTimes updates LiveData`() {
        val observer = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.isHideExactTimes.observeForever(observer)

        viewModel.setHideExactTimes(true)

        verify { observer.onChanged(true) }
    }
}
