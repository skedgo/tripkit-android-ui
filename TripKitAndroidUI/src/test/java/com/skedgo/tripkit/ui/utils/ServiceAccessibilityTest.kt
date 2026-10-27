package com.skedgo.tripkit.ui.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.DefaultTripPreferences
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.tripresults.PrefsBasedTransportViewFilter
import com.skedgo.tripkit.ui.tripresults.PREF_KEY_IS_DEFAULT_CONFIG_SET
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

    @Test fun `wheelchair mode enables information and shows all accessibility states`() {
        val filter = PrefsBasedTransportViewFilter(context)
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, true)
        assertTrue(tripPreferences.isWheelchairPreferred())
        assertEquals(R.drawable.ic_wheelchair, policy.wheelchairIcon(true))
        assertEquals(R.drawable.ic_wheelchair_not_accessible, policy.wheelchairIcon(false))
        assertEquals(R.drawable.ic_wheelchair_unknown, policy.wheelchairIcon(null))
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, false)
        assertNull(policy.wheelchairIcon(true))
        assertNull(policy.wheelchairIcon(null))
        assertEquals(R.drawable.ic_wheelchair_not_accessible, policy.wheelchairIcon(false))
    }

    @Test fun `information preference still shows unknown when wheelchair routing is off`() {
        val filter = PrefsBasedTransportViewFilter(context)
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, false)
        tripPreferences.setWheelchairPreferred(true)
        assertFalse(filter.isSelected(TransportMode.ID_WHEEL_CHAIR))
        assertEquals(R.drawable.ic_wheelchair_unknown, policy.wheelchairIcon(null))
    }

    @Test fun `deselecting walking selects wheelchair and enables information`() {
        val filter = PrefsBasedTransportViewFilter(context)
        filter.setSelected(TransportMode.ID_WALK, false)
        assertTrue(filter.isSelected(TransportMode.ID_WHEEL_CHAIR))
        assertTrue(tripPreferences.isWheelchairPreferred())
        assertEquals(R.drawable.ic_wheelchair_unknown, policy.wheelchairIcon(null))
    }

    @Test fun `existing wheelchair selection initializes missing information when rows reopen`() {
        modes.edit().putBoolean(PREF_KEY_IS_DEFAULT_CONFIG_SET, true)
            .putBoolean(TransportMode.ID_WALK, false).putBoolean("cy_bic", true).commit()
        val reopened = ServiceAccessibility(context)
        assertTrue(tripPreferences.isWheelchairPreferred())
        assertEquals(R.drawable.ic_wheelchair_unknown, reopened.wheelchairIcon(null))
        assertTrue(reopened.showBicycle(true))
    }

    @Test fun `opening transport settings initializes missing information for existing wheelchair selection`() {
        modes.edit().putBoolean(PREF_KEY_IS_DEFAULT_CONFIG_SET, true)
            .putBoolean(TransportMode.ID_WALK, false).commit()
        val filter = PrefsBasedTransportViewFilter(context)
        assertTrue(filter.isSelected(TransportMode.ID_WHEEL_CHAIR))
        assertTrue(tripPreferences.isWheelchairPreferred())
    }

    @Test fun `saved manual information off survives reopening with wheelchair selected`() {
        val filter = PrefsBasedTransportViewFilter(context)
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, true)
        tripPreferences.setWheelchairPreferred(false)
        val reopenedFilter = PrefsBasedTransportViewFilter(context)
        val reopened = ServiceAccessibility(context)
        assertTrue(reopenedFilter.isSelected(TransportMode.ID_WHEEL_CHAIR))
        assertFalse(tripPreferences.isWheelchairPreferred())
        assertNull(reopened.wheelchairIcon(null))
        assertEquals(R.drawable.ic_wheelchair_not_accessible, reopened.wheelchairIcon(false))
    }

    @Test fun `information can be disabled after mode selection and walking clears it`() {
        val filter = PrefsBasedTransportViewFilter(context)
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, true)
        tripPreferences.setWheelchairPreferred(false)
        assertTrue(filter.isSelected(TransportMode.ID_WHEEL_CHAIR))
        assertNull(policy.wheelchairIcon(null))
        tripPreferences.setWheelchairPreferred(true)
        filter.setSelected(TransportMode.ID_WALK, true)
        assertFalse(tripPreferences.isWheelchairPreferred())
        assertNull(policy.wheelchairIcon(null))
        filter.setSelected("cy_bic", false)
        assertFalse(tripPreferences.isWheelchairPreferred())
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
