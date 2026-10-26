package com.skedgo.tripkit.ui.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.DefaultTripPreferences
import com.skedgo.tripkit.ui.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceAccessibilityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val tripPreferences = DefaultTripPreferences(context.getSharedPreferences("TripPreferences", 0))
    private val modes = context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, 0)
    private lateinit var policy: ServiceAccessibility

    @org.junit.After fun teardown() { io.reactivex.plugins.RxJavaPlugins.reset() }

    @Before fun setup() {
        io.reactivex.plugins.RxJavaPlugins.setIoSchedulerHandler { io.reactivex.schedulers.Schedulers.trampoline() }
        context.getSharedPreferences("TripPreferences", 0).edit().clear().commit()
        modes.edit().clear().commit()
        policy = ServiceAccessibility(context)
    }

    @Test fun `wheelchair information on preserves three different icons`() {
        tripPreferences.setWheelchairPreferred(true)
        assertEquals(R.drawable.ic_wheelchair, policy.wheelchairIcon(true))
        assertEquals(R.drawable.ic_wheelchair_not_accessible, policy.wheelchairIcon(false))
        assertEquals(R.drawable.ic_wheelchair_unknown, policy.wheelchairIcon(null))
    }

    @Test fun `wheelchair information off hides positive and unknown only`() {
        assertNull(policy.wheelchairIcon(true))
        assertNull(policy.wheelchairIcon(null))
        assertEquals(R.drawable.ic_wheelchair_not_accessible, policy.wheelchairIcon(false))
    }

    @Test fun `wheelchair routing mode does not enable information`() {
        modes.edit().putBoolean("wa_wal", false).commit()
        assertNull(policy.wheelchairIcon(true))
        assertNull(policy.wheelchairIcon(null))
    }

    @Test fun `bike requires support even when a mode is visible`() {
        assertTrue(policy.showBicycle(true))
        assertFalse(policy.showBicycle(false))
        assertFalse(policy.showBicycle(null))
    }

    @Test fun `hidden bike and micromobility hide bike information`() {
        modes.edit().putBoolean("cy_bic", false).putBoolean("me_mic", false)
            .putBoolean("me_mic-s", false).commit()
        assertFalse(policy.showBicycle(true))
    }

    @Test fun `visible micromobility permits bike information with legacy bike hidden`() {
        modes.edit().putBoolean("cy_bic", false).putBoolean("me_mic_e-sco", true).commit()
        assertTrue(policy.showBicycle(true))
        modes.edit().putBoolean("me_mic_e-sco", false).putBoolean("me_mic-s_provider", true).commit()
        assertTrue(policy.showBicycle(true))
    }

    @Test fun `unrelated visible modes do not permit bike information`() {
        modes.edit().putBoolean("cy_bic", false).putBoolean("me_car", true).commit()
        assertFalse(policy.showBicycle(true))
    }

    @Test fun `preference changes notify active views and dispose cleanly`() {
        val changes = policy.changes.test()
        tripPreferences.setWheelchairPreferred(true)
        modes.edit().putBoolean("cy_bic", false).commit()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        changes.awaitCount(2).assertValueCount(2)
        assertEquals(R.drawable.ic_wheelchair_unknown, policy.wheelchairIcon(null))
        assertFalse(policy.showBicycle(true))
        changes.dispose()
        tripPreferences.setWheelchairPreferred(false)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        changes.assertValueCount(2)
    }
}
