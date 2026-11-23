package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.DefaultTripPreferences
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceDetailItemViewModelAccessibilityTest : MockKTest() {
    @get:Rule val rule = InstantTaskExecutorRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences =
        DefaultTripPreferences(context.getSharedPreferences("TripPreferences", 0))
    private val getStopTimeDisplayText: GetStopTimeDisplayText = mockk(relaxed = true)
    private lateinit var viewModel: ServiceDetailItemViewModel

    @Before fun setup() {
        initRx()
        context.getSharedPreferences("TripPreferences", 0).edit().clear().commit()
        every { getStopTimeDisplayText.execute(any()) } returns Observable.just("12:30")
        viewModel = ServiceDetailItemViewModel(getStopTimeDisplayText)
    }

    @After fun teardown() {
        tearDownRx()
    }

    private fun bind(accessible: Boolean?) {
        val stop = mockk<ServiceStop>(relaxed = true)
        every { stop.wheelchairAccessible } returns accessible
        viewModel.setStop(context, stop, _lineColor = 0, travelled = false)
    }

    @Test fun `stop rows follow the same show and hide rule as the rest of the app`() {
        for (information in listOf(true, false)) {
            preferences.setWheelchairPreferred(information)
            for (status in listOf(true, false, null)) {
                bind(status)
                assertEquals(
                    "information=$information status=$status",
                    status == false || information,
                    viewModel.wheelchairIcon.value != null
                )
            }
        }
    }

    @Test fun `stop rows describe each wheelchair state`() {
        preferences.setWheelchairPreferred(true)
        bind(true)
        assertEquals(context.getString(R.string.wheelchair_accessible), viewModel.wheelchairAccessibleText.value)
        bind(false)
        assertEquals(context.getString(R.string.not_wheelchair_accessible), viewModel.wheelchairAccessibleText.value)
        bind(null)
        assertEquals(context.getString(R.string.wheelchair_accessibility_unknown), viewModel.wheelchairAccessibleText.value)
    }

    @Test fun `recycled stop rows clear the previous icon`() {
        preferences.setWheelchairPreferred(false)
        bind(false)
        assertEquals(true, viewModel.wheelchairIcon.value != null)
        bind(null)
        assertEquals(null, viewModel.wheelchairIcon.value)
        assertEquals(null, viewModel.wheelchairAccessibleText.value)
    }
}
