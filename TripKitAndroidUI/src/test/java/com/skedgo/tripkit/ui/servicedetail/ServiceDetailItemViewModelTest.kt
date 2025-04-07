package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.graphics.drawable.NinePatchDrawable
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceDetailItemViewModelTest: MockKTest() {

    private lateinit var viewModel: ServiceDetailItemViewModel
    private val getStopTimeDisplayText: GetStopTimeDisplayText = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val stop: ServiceStop = mockk(relaxed = true)
    private val ninePatchDrawable: NinePatchDrawable = mockk(relaxed = true)

    @Before
    fun setup() {
        initRx()

        viewModel = ServiceDetailItemViewModel(getStopTimeDisplayText)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `setStop should update stop details and scheduledTime`() {
        every { stop.name } returns "Test Stop"
        every { getStopTimeDisplayText.execute(stop) } returns Observable.just("12:30 PM")

        viewModel.setStop(context, stop, _lineColor = 0xFF0000.toInt(), travelled = true)

        assertEquals("Test Stop", viewModel.stopName.get())
        assertEquals("12:30 PM", viewModel.scheduledTime.get())

        verify { getStopTimeDisplayText.execute(stop) }
    }

    @Test
    fun `setDrawable should update lineDrawable based on LineDirection`() {
        every { ContextCompat.getDrawable(context, R.drawable.service_line_start) } returns ninePatchDrawable
        every { ContextCompat.getDrawable(context, R.drawable.service_line_middle) } returns ninePatchDrawable
        every { ContextCompat.getDrawable(context, R.drawable.service_line_end) } returns ninePatchDrawable

        // Test each direction
        viewModel.setDrawable(context, ServiceDetailItemViewModel.LineDirection.START)
        assertEquals(ninePatchDrawable, viewModel.lineDrawable.get())

        viewModel.setDrawable(context, ServiceDetailItemViewModel.LineDirection.MIDDLE)
        assertEquals(ninePatchDrawable, viewModel.lineDrawable.get())

        viewModel.setDrawable(context, ServiceDetailItemViewModel.LineDirection.END)
        assertEquals(ninePatchDrawable, viewModel.lineDrawable.get())
    }
}