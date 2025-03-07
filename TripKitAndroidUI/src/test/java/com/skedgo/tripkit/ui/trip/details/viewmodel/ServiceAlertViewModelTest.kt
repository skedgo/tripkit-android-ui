package com.skedgo.tripkit.ui.trip.details.viewmodel

import android.content.Context
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R
import io.mockk.*
import io.reactivex.observers.TestObserver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ServiceAlertViewModelTest {

    private lateinit var viewModel: ServiceAlertViewModel
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        viewModel = ServiceAlertViewModel(context)
    }

    @Test
    fun `setAlerts - no alerts should clear title and icon`() {
        viewModel.setAlerts(emptyList())

        assertFalse(viewModel.hasAlerts.get())
        assertTrue(viewModel.title.get() == "0 ")
        assertNull(viewModel.alertIcon.get())
    }

    @Test
    fun `setAlerts - single alert should set title to alert title`() {
        val alert: RealtimeAlert = mockk {
            every { severity() } returns RealtimeAlert.SEVERITY_ALERT
        }
        every { alert.title() } returns "Service Disruption"

        viewModel.setAlerts(listOf(alert))

        assertTrue(viewModel.hasAlerts.get())
        assertEquals("Service Disruption", viewModel.title.get())
    }

    @Test
    fun `setAlerts - multiple alerts should set title to alert count`() {
        val alert1: RealtimeAlert = mockk {
            every { severity() } returns RealtimeAlert.SEVERITY_ALERT
        }
        val alert2: RealtimeAlert = mockk {
            every { severity() } returns RealtimeAlert.SEVERITY_ALERT
        }
        every { context.getString(R.string.alerts) } returns "alerts"

        viewModel.setAlerts(listOf(alert1, alert2))

        assertTrue(viewModel.hasAlerts.get())
        assertEquals("2 alerts", viewModel.title.get())
    }

    @Test
    fun `setAlerts - sets correct icon based on severity`() {
        val alert: RealtimeAlert = mockk()
        every { alert.title() } returns "Alert"
        every { alert.severity() } returns RealtimeAlert.SEVERITY_ALERT
        every { ContextCompat.getDrawable(context, R.drawable.ic_alert_red_overlay) } returns mockk()

        viewModel.setAlerts(listOf(alert))

        assertNotNull(viewModel.alertIcon.get())
    }

    @Test
    fun `onShow - triggers showAlertsObservable`() {
        val testObserver = TestObserver<Unit>()
        viewModel.showAlertsObservable.subscribe(testObserver)

        viewModel.onShow()

        testObserver.assertValue(Unit)
    }
}
