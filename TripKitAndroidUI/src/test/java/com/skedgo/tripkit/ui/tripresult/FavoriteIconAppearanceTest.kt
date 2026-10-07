package com.skedgo.tripkit.ui.tripresult

import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.ui.R
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
class FavoriteIconAppearanceTest {
    @Test
    fun `neutral tint preserves outline versus filled favorite state`() {
        val context = RuntimeEnvironment.getApplication()
        val outline = ContextCompat.getDrawable(context, R.drawable.ic_favorite_outline)!!.mutate()
        val filled = ContextCompat.getDrawable(context, R.drawable.ic_favorite_new)!!.mutate()
        outline.setTint(Color.BLACK)
        filled.setTint(Color.BLACK)

        val inactive = outline.toBitmap(48, 48)
        val active = filled.toBitmap(48, 48)
        assertThat(Color.alpha(inactive.getPixel(24, 24))).isZero()
        assertThat(Color.alpha(active.getPixel(24, 24))).isEqualTo(255)
        assertThat((0 until 48).any { x ->
            (0 until 48).any { y -> Color.alpha(inactive.getPixel(x, y)) > 0 }
        }).isTrue()
    }
}
