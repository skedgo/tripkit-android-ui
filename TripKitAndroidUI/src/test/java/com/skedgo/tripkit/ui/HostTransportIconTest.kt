package com.skedgo.tripkit.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.ui.utils.hostTransportIcon
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HostTransportIconTest {
    private fun host(dark: Boolean) = ContextThemeWrapper(ApplicationProvider.getApplicationContext(),
        if (dark) com.google.android.material.R.style.Theme_MaterialComponents_NoActionBar
        else com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)

    @Test fun `light host corrects a white application-tinted icon without mutating its source`() {
        val source = ColorDrawable(Color.BLACK).apply { setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }
        val context = host(false)
        val result = hostTransportIcon(context, source, false, false)
        val expected = com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, "test")
        assertEquals(expected, org.robolectric.Shadows.shadowOf(result.colorFilter as PorterDuffColorFilter).color)
        assertEquals(Color.WHITE, org.robolectric.Shadows.shadowOf(source.colorFilter as PorterDuffColorFilter).color)
    }

    @Test fun `dark host chooses its own contrasting foreground`() {
        val context = host(true)
        val result = hostTransportIcon(context, ColorDrawable(Color.BLACK), false, false)
        val expected = com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, "test")
        assertEquals(expected, org.robolectric.Shadows.shadowOf(result.colorFilter as PorterDuffColorFilter).color)
    }

    @Test fun `branding service colors and icons on service circles retain their rendering`() {
        val source = ColorDrawable(Color.MAGENTA)
        assertSame(source, hostTransportIcon(host(false), source, true, false))
        assertSame(source, hostTransportIcon(host(false), source, false, true))
    }

    @Test fun `alert overlay keeps its colors when transport layer is corrected`() {
        val source = LayerDrawable(arrayOf(ColorDrawable(Color.WHITE), ColorDrawable(Color.YELLOW)))
        val result = hostTransportIcon(host(false), source, false, false, hasAlertOverlay = true) as LayerDrawable
        assertNotNull(result.getDrawable(0).colorFilter)
        assertNull(result.getDrawable(1).colorFilter)
        assertNull(source.getDrawable(0).colorFilter)
    }
    @Test fun `preview header uses host foreground without mutating its source`() {
        val context = host(true)
        val binding = com.skedgo.tripkit.ui.databinding.TripPreviewPagerDirectionsItemBinding.inflate(android.view.LayoutInflater.from(context))
        val model = com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemViewModel()
        val source = ColorDrawable(Color.BLACK)
        model.icon.set(source)
        binding.viewModel = model
        binding.executePendingBindings()
        val icon = binding.root.findViewById<android.widget.ImageView>(R.id.icon)
        assertNotNull(icon.drawable.colorFilter)
        val expected = com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, "test")
        assertEquals(expected, org.robolectric.Shadows.shadowOf(icon.drawable.colorFilter as PorterDuffColorFilter).color)
        assertNull(source.colorFilter)
    }

}
