package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.skedgo.tripkit.ui.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ActionButtonComposeTest {
    private fun originalStyleContext(night: Boolean): Context =
        ContextThemeWrapper(context(night), R.style.TripActionButtonOriginal)

    @Test
    fun `original primary appearance retains day night label resource`() {
        for (night in listOf(false, true)) {
            val context = originalStyleContext(night)
            val green = Color.rgb(35, 177, 94)
            assertThat(actionButtonForegroundColor(context, true, green))
                .isEqualTo(ContextCompat.getColor(context, R.color.white))
        }
    }

    @Test
    fun `original primary icons retain their view model tint independently of label color`() {
        for (night in listOf(false, true)) {
            val context = originalStyleContext(night)
            val label = ContextCompat.getColor(context, R.color.white)
            assertThat(actionButtonIconTint(context, true, true, Color.WHITE, label)).isEqualTo(Color.WHITE)
        }
    }

    @Test
    fun `default branded primary and quick book foregrounds remain unchanged`() {
        for (night in listOf(false, true)) {
            val context = context(night)
            val foreground = actionButtonForegroundColor(context, true, Color.rgb(95, 67, 154))
            assertThat(foreground).isEqualTo(Color.WHITE)
            assertThat(actionButtonIconTint(context, true, true, Color.WHITE, foreground)).isEqualTo(Color.WHITE)
        }
    }
    private fun context(night: Boolean): Context {
        val app = RuntimeEnvironment.getApplication()
        val configuration = Configuration(app.resources.configuration)
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return app.createConfigurationContext(configuration)
    }

    @Test
    fun `primary labels stay readable against green and purple brand fills in both themes`() {
        for (night in listOf(false, true)) {
            for (background in listOf(Color.rgb(26, 181, 92), Color.rgb(95, 67, 154), Color.rgb(105, 45, 145))) {
                val foreground = actionButtonForegroundColor(context(night), true, background)
                assertThat(ColorUtils.calculateContrast(foreground, background))
                    .describedAs("primary contrast night=%s background=%s", night, Integer.toHexString(background))
                    .isGreaterThanOrEqualTo(4.5)
            }
        }
    }

    @Test
    fun `secondary favorite and mute labels keep their theme semantic foreground`() {
        for (night in listOf(false, true)) {
            val context = context(night)
            val surface = ContextCompat.getColor(context, R.color.inputBackground)
            val foreground = actionButtonForegroundColor(context, false, surface)
            assertThat(foreground).isEqualTo(ContextCompat.getColor(context, R.color.labelPrimary))
            assertThat(ColorUtils.calculateContrast(foreground, surface)).isGreaterThanOrEqualTo(4.5)
        }
    }
}
