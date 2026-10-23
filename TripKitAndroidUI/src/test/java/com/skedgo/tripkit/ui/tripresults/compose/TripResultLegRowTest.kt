package com.skedgo.tripkit.ui.tripresults.compose

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
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
class TripResultLegRowTest {
    @Test
    fun `quick booking foreground is readable on branded fills in both themes`() {
        for (qualifier in listOf("notnight", "night")) {
            RuntimeEnvironment.setQualifiers(qualifier)
            val context = RuntimeEnvironment.getApplication()
            for (fill in listOf(Color.rgb(95, 67, 154), Color.rgb(26, 181, 92))) {
                assertThat(androidx.core.graphics.ColorUtils.calculateContrast(
                    quickBookingForegroundColor(context, fill), fill
                )).isGreaterThanOrEqualTo(4.5)
            }
        }
    }

    private fun icon(color: Int): BitmapDrawable {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return BitmapDrawable(RuntimeEnvironment.getApplication().resources, bitmap)
    }

    @Test
    fun `template glyph renders applied dark and light tints rather than the original bitmap`() {
        val icon = icon(Color.BLACK)
        for (tint in listOf(Color.WHITE, Color.DKGRAY)) {
            icon.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
            assertThat(icon.tripDrawableBitmap().getPixel(4, 4)).isEqualTo(tint)
        }
    }

    @Test
    fun `untinted branding artwork retains its original color and bounds`() {
        val icon = icon(Color.MAGENTA)
        icon.setBounds(2, 3, 10, 11)
        val bounds = android.graphics.Rect(icon.bounds)
        assertThat(icon.tripDrawableBitmap().getPixel(4, 4)).isEqualTo(Color.MAGENTA)
        assertThat(icon.bounds).isEqualTo(bounds)
    }
}
