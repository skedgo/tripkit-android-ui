package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.ui.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BadgeAndBikeColorsTest {
    private fun context(night: Boolean): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration).apply {
            uiMode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        return ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.AppTheme)
    }

    @Test fun `badges choose the greater contrast including translucent fills in both themes`() {
        for (night in listOf(false, true)) {
            val context = context(night)
            val parent = android.widget.FrameLayout(context).apply { setBackgroundColor(context.getColor(R.color.white)) }
            val text = TextView(context)
            parent.addView(text)
            for (fill in listOf(0xFF663399.toInt(), 0xFF005C60.toInt(), Color.YELLOW, Color.WHITE, Color.BLACK, 0x40000000, Color.TRANSPARENT)) {
                bindReadableBadgeTextColor(text, fill)
                val surface = ColorUtils.compositeColors(fill, context.getColor(R.color.white))
                val expected = if (ColorUtils.calculateContrast(Color.WHITE, surface) >= ColorUtils.calculateContrast(Color.BLACK, surface)) Color.WHITE else Color.BLACK
                assertEquals("night=$night fill=$fill", expected, text.currentTextColor)
                assertTrue(ColorUtils.calculateContrast(text.currentTextColor, surface) >= 4.5)
            }
        }
    }

    @Test fun `XML card badges composite their fill exactly once`() {
        val context = context(false)
        val container = android.widget.FrameLayout(context).apply { setBackgroundColor(Color.WHITE) }
        val card = androidx.cardview.widget.CardView(context)
        container.addView(card)
        val text = TextView(context)
        card.addView(text)
        val fill = 0x80000000.toInt()
        card.setCardBackgroundColor(fill)
        bindReadableBadgeTextColor(text, fill)
        assertEquals(Color.BLACK, text.currentTextColor)
    }

    @Test fun `bike station foregrounds resolve with the popup surface in both themes`() {
        for (night in listOf(false, true)) {
            val context = context(night)
            val root = LayoutInflater.from(context).inflate(R.layout.bike_share_info_window, null) as ViewGroup
            val surface = (root.background as ColorDrawable).color
            for (id in listOf(R.id.operatorName, R.id.availableBikesText, R.id.availableBikes, R.id.batteryLevelText, R.id.batteryLevel)) {
                val text = root.findViewById<TextView>(id)
                assertTrue("night=$night text=$id", ColorUtils.calculateContrast(text.currentTextColor, surface) >= 4.5)
            }
            val icons = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<ImageView>()
            assertEquals(2, icons.size)
            for (icon in icons) {
                assertNotNull("night=$night icon tint", icon.imageTintList)
                assertEquals(context.getColor(R.color.labelPrimary), icon.imageTintList!!.defaultColor)
                assertTrue(ColorUtils.calculateContrast(icon.imageTintList!!.defaultColor, surface) >= 3.0)
            }
        }
    }
}
