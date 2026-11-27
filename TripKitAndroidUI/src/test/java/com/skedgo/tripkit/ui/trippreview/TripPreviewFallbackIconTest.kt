package com.skedgo.tripkit.ui.trippreview

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.tripDrawableBitmap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TripPreviewFallbackIconTest {
    @Test
    fun `local bicycle fallback follows the label foreground in dark and light themes`() {
        val method = TripPreviewPagerItemViewModel::class.java.getDeclaredMethod(
            "getLegacyFallbackIcon", android.content.Context::class.java, TripSegment::class.java
        ).apply { isAccessible = true }
        for (qualifier in listOf("night", "notnight")) {
            RuntimeEnvironment.setQualifiers(qualifier)
            val context = RuntimeEnvironment.getApplication()
            val segment = TripSegment().apply { transportModeId = TransportMode.ID_BICYCLE }
            val drawable = method.invoke(TripPreviewPagerItemViewModel(), context, segment) as Drawable
            val bitmap = drawable.tripDrawableBitmap()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val foreground = pixels.filter { Color.alpha(it) == 255 }.map { it and 0xffffff }
            assertThat(foreground).isNotEmpty()
            val expected = ContextCompat.getColor(context, R.color.labelPrimary) and 0xffffff
            assertThat(foreground.toSet()).containsExactly(expected)
        }
    }
    @Test
    fun `retained Preview model refreshes its fallback icon with the resumed theme`() {
        val viewModel = TripPreviewPagerItemViewModel().apply {
            segment = TripSegment().apply { transportModeId = TransportMode.ID_BICYCLE }
        }
        for (qualifier in listOf("notnight", "night", "notnight")) {
            RuntimeEnvironment.setQualifiers(qualifier)
            val context = RuntimeEnvironment.getApplication()
            viewModel.refreshIcon(context)
            val bitmap = viewModel.icon.get()!!.tripDrawableBitmap()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val opaque = pixels.filter { Color.alpha(it) == 255 }.map { it and 0xffffff }.toSet()
            assertThat(opaque).containsExactly(ContextCompat.getColor(context, R.color.labelPrimary) and 0xffffff)
        }
        viewModel.onCleared()
    }

}
