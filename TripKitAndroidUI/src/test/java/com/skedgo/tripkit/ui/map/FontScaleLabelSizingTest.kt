package com.skedgo.tripkit.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Redmine #19351: the map time label and the timetable "Go To Now" button used fixed heights and
 * clipped their text at the largest system font size. They must keep their normal height at
 * font scale 1.0 and grow to fit the text at 2.0.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FontScaleLabelSizingTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private fun textLineHeight(view: TextView) =
        view.paint.fontMetricsInt.let { it.bottom - it.top }

    @Test
    fun `time label keeps its minimum height at normal font scale`() {
        val textView = LayoutInflater.from(context).inflate(R.layout.view_time_label, null) as TextView
        val bitmap = TimeLabelMaker(textView).create(1_700_000_000_000L, "UTC")

        val minHeight = context.resources.getDimensionPixelSize(R.dimen.time_label_height)
        assertEquals(maxOf(minHeight, textLineHeight(textView)), bitmap.height)
    }

    @Test
    @Config(fontScale = 2.0f)
    fun `time label grows to fit its text at maximum font scale`() {
        val textView = LayoutInflater.from(context).inflate(R.layout.view_time_label, null) as TextView
        val bitmap = TimeLabelMaker(textView).create(1_700_000_000_000L, "UTC")

        assertTrue(
            "label height ${bitmap.height} must fit text line ${textLineHeight(textView)}",
            bitmap.height >= textLineHeight(textView)
        )
        assertTrue(bitmap.height >= context.resources.getDimensionPixelSize(R.dimen.time_label_height))
    }

    private fun measureGoToNowButton(): Button {
        val themed = android.view.ContextThemeWrapper(context, R.style.ToNowButtonStyle)
        val button = Button(themed, null, 0, R.style.ToNowButtonStyle).apply {
            text = context.getString(R.string.go_to_now)
        }
        button.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        return button
    }

    @Test
    fun `go to now button keeps 40dp minimum height at normal font scale`() {
        val button = measureGoToNowButton()

        assertEquals(dp(40), button.minHeight)
        assertEquals(
            maxOf(dp(40), textLineHeight(button) + button.paddingTop + button.paddingBottom),
            button.measuredHeight
        )
    }

    @Test
    @Config(fontScale = 2.0f)
    fun `go to now button grows to fit its text at maximum font scale`() {
        val button = measureGoToNowButton()

        assertTrue(
            "button height ${button.measuredHeight} must fit text line ${textLineHeight(button)}",
            button.measuredHeight >= textLineHeight(button) + button.paddingTop + button.paddingBottom
        )
    }
}
