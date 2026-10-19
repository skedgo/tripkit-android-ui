package com.skedgo.tripkit.ui.utils

import android.content.res.Configuration
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.account.data.AppColor
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.TripResultListFragmentBinding
import com.skedgo.tripkit.ui.databinding.TripResultListItemBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Ticket26294ContrastTest {
    private val purple = Color.rgb(95, 67, 154)
    private fun inflater(night: Boolean): LayoutInflater {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = Configuration(app.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        val ctx = ContextThemeWrapper(app.createConfigurationContext(config), R.style.AppTheme)
        DynamicAppColor.initAppColors(AppColor(red = 95, green = 67, blue = 154), AppColor(255,255,255), AppColor(red = 95, green = 67, blue = 154))
        return LayoutInflater.from(ctx)
    }
    private fun readable(text: TextView, surface: Int) {
        val ratio = ColorUtils.calculateContrast(text.currentTextColor, surface)
        val label = if (text.id == android.view.View.NO_ID) "departureChipText" else text.resources.getResourceEntryName(text.id)
        println("26294 $label foreground=${Integer.toHexString(text.currentTextColor)} surface=${Integer.toHexString(surface)} contrast=$ratio")
        assertTrue("text contrast must be at least 4.5:1; color=" + Integer.toHexString(text.currentTextColor),
            ColorUtils.calculateContrast(text.currentTextColor, surface) >= 4.5)
    }
    @Test fun `dark current location and learn more remain readable with remote purple`() {
        val b = TripResultListFragmentBinding.inflate(inflater(true))
        b.executePendingBindings()
        val surface = b.root.resources.getColor(R.color.white, b.root.context.theme)
        readable(b.from, surface)
        readable(b.tvLearnMore, surface)
    }
    @Test fun `departure chip does not paint purple text on solid purple`() {
        val b = TripResultListFragmentBinding.inflate(inflater(true))
        b.executePendingBindings()
        val text = b.leaveNowLayout.getChildAt(0) as TextView
        val base = b.root.resources.getColor(R.color.white, b.root.context.theme)
        readable(text, base)
        assertEquals("Both header actions use the same outline tint treatment",
            b.transportsLayout.backgroundTintList, b.leaveNowLayout.backgroundTintList)
    }
    @Test fun `light theme retains remote brand action color`() {
        val b = TripResultListFragmentBinding.inflate(inflater(false))
        b.executePendingBindings()
        assertEquals(purple, b.from.currentTextColor)
    }
    @Test fun `dark more action is readable on its card`() {
        val b = TripResultListItemBinding.inflate(inflater(true))
        b.executePendingBindings()
        b.moreButton.isEnabled = true
        val surface = (b.root as androidx.cardview.widget.CardView).cardBackgroundColor.defaultColor
        readable(b.moreButton, surface)
    }
}
