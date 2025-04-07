package com.skedgo.tripkit.ui.trippreview.v2

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.TurnByTurn
import com.skedgo.tripkit.routing.TurnByTurn.CYCLING
import com.skedgo.tripkit.ui.utils.correctItemType
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TripPreviewParentViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule() // Executes LiveData instantly

    private lateinit var viewModel: TripPreviewParentViewModel
    @Before
    fun setUp() {
        viewModel = TripPreviewParentViewModel()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setToolbarState should post correct toolbar state`() {
        val toolbarState = PreviewToolbarState.OnPreview(
            backActionLabel = "Back",
            title = "Trip Preview"
        )

        val observer = mockk<Observer<PreviewToolbarState>>(relaxed = true)
        viewModel.toolbarState.observeForever(observer)

        viewModel.setToolbarState(toolbarState)

        verify {
            observer.onChanged(toolbarState)
        }
    }

    @Test
    fun `setTripSegment should post trip segment and update segment item type`() {

        val mockTripSegment = spyk(TripSegment()) {
            setTurnByTurn(CYCLING.name)
            every { from } returns mockk()
            every { to } returns mockk()
            every { getType() } returns SegmentType.SCHEDULED
        }

        mockkStatic("com.skedgo.tripkit.ui.utils.TripSegmentExtensionsKt")
        every { mockTripSegment.correctItemType() } returns 123

        val observer = mockk<Observer<Int>>(relaxed = true)
        viewModel.segmentItemType.observeForever(observer)

        viewModel.setTripSegment(mockTripSegment)

        verify { mockTripSegment.correctItemType() }
        verify { observer.onChanged(123) }
    }
}
