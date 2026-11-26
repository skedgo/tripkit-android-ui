package com.skedgo.tripkit.ui.trippreview.drt

import android.app.Activity
import android.app.Application
import android.graphics.Rect
import android.os.Looper
import android.graphics.drawable.InsetDrawable
import android.view.MotionEvent
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ItemDrtTicketBinding
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DrtTicketTouchTargetTest {
    @Test
    fun `quantity actions have separate minimum accessible touch areas`() {
        listOf("notnight", "night").forEach { mode ->
            RuntimeEnvironment.setQualifiers(mode)
            assertTouchTargets()
        }
    }

    @Test
    fun `fixed mandatory fare binds both quantity actions disabled`() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.AppTheme)
        val binding = ItemDrtTicketBinding.inflate(LayoutInflater.from(context))
        val fare = com.google.gson.Gson().fromJson(
            """{"id":"opaque","min":1,"max":1}""",
            com.skedgo.tripkit.booking.quickbooking.Fare::class.java
        )
        binding.viewModel = DrtTicketViewModel().apply { setTicket(fare); setViewMode(false) }
        binding.executePendingBindings()
        assertThat(binding.itemDrtIbDecrement.isEnabled).isFalse()
        assertThat(binding.itemDrtIbIncrement.isEnabled).isFalse()
        assertThat(binding.itemDrtIbDecrement.alpha).isEqualTo(0.4f)
    }

    private fun assertTouchTargets() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.AppTheme)
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val binding = ItemDrtTicketBinding.inflate(LayoutInflater.from(context))
        controller.get().setContentView(binding.root)
        binding.viewModel = DrtTicketViewModel().apply { setValue(1); setViewMode(false) }
        binding.executePendingBindings()
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        binding.root.layout(0, 0, binding.root.measuredWidth, binding.root.measuredHeight)
        val minimum = (48 * context.resources.displayMetrics.density).toInt()
        listOf(binding.itemDrtIbDecrement, binding.itemDrtIbIncrement).forEach {
            assertThat(it.measuredWidth).isGreaterThanOrEqualTo(minimum)
            assertThat(it.measuredHeight).isGreaterThanOrEqualTo(minimum)
            // The purple surface stays 24dp, independently of the 48dp clickable view.
            assertThat(it.background).isInstanceOf(InsetDrawable::class.java)
            val surface = (it.background as InsetDrawable).drawable!!
            it.background.setBounds(0, 0, it.width, it.height)
            val compact = (24 * context.resources.displayMetrics.density).toInt()
            assertThat(surface.bounds.width()).isEqualTo(compact)
            assertThat(surface.bounds.height()).isEqualTo(compact)
            assertThat(it.width - it.paddingLeft - it.paddingRight)
                .isEqualTo((8 * context.resources.displayMetrics.density).toInt())
            assertThat(it.height - it.paddingTop - it.paddingBottom)
                .isEqualTo((8 * context.resources.displayMetrics.density).toInt())
        }
        assertThat(binding.itemDrtIbDecrement.right).isLessThanOrEqualTo(binding.itemDrtIbIncrement.left)
        var decreases = 0
        var increases = 0
        binding.itemDrtIbDecrement.setOnClickListener { decreases++ }
        binding.itemDrtIbIncrement.setOnClickListener { increases++ }
        fun tap(view: View) {
            val bounds = Rect()
            view.getDrawingRect(bounds)
            (binding.root as ViewGroup).offsetDescendantRectToMyCoords(view, bounds)
            val x = (bounds.left + 2).toFloat()
            val y = (bounds.top + 2).toFloat()
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                val event = MotionEvent.obtain(0, 10, action, x, y, 0)
                binding.root.dispatchTouchEvent(event)
                event.recycle()
            }
            shadowOf(Looper.getMainLooper()).idle()
        }
        tap(binding.itemDrtIbDecrement)
        tap(binding.itemDrtIbIncrement)
        assertThat(decreases).isEqualTo(1)
        assertThat(increases).isEqualTo(1)
        binding.itemDrtIbIncrement.isEnabled = false
        tap(binding.itemDrtIbIncrement)
        assertThat(increases).isEqualTo(1)
        assertThat(decreases).isEqualTo(1)
        controller.pause().stop().destroy()
    }
}
