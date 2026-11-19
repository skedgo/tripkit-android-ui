package com.skedgo.tripkit.ui

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HostThemeIntegrationTest {
    private fun checkSurface(theme: Int) {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), theme)
        val values = context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorSurface))
        val expected = values.getColor(0, 0)
        values.recycle()
        for (layout in listOf(R.layout.fragment_tkui_route_compose, R.layout.fragment_tkui_trip_results,
            R.layout.fragment_tkui_trip_details_view_controller, R.layout.fragment_tkui_trip_preview)) {
            val root = LayoutInflater.from(context).inflate(layout, null)
            assertEquals("Controller shell must use host surface", expected, ((if (layout == R.layout.fragment_tkui_trip_preview) root.findViewById<android.view.View>(R.id.vpTripPreview) else root).background as ColorDrawable).color)
        }
    }

    @Test fun `light host supplies controller surfaces`() =
        checkSurface(com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)

    @Test fun `dark host supplies controller surfaces`() =
        checkSurface(com.google.android.material.R.style.Theme_MaterialComponents_NoActionBar)
}
