package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.tripDrawableBitmap
import io.mockk.every
import io.mockk.mockk
import com.skedgo.tripkit.ui.tripresults.TripSegmentHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TripSegmentIconAppearanceTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private fun assertIcon(segment: TripSegment, night: Boolean, circle: Boolean = false) {
        val app = RuntimeEnvironment.getApplication()
        val config = Configuration(app.resources.configuration).apply {
            uiMode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val context = app.createConfigurationContext(config)
        val helper = mockk<TripSegmentHelper> { every { getIconUrlForModeInfo(any(), any()) } returns null }
        val vm = TripSegmentItemViewModel(context, mockk(), helper, mockk(), mockk(), mockk())
        TripSegmentItemViewModel::class.java.getDeclaredMethod("showSegmentIcon", TripSegment::class.java, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(vm, segment, circle)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val bitmap = requireNotNull(vm.icon.value).tripDrawableBitmap()
        val expected = if (circle) Color.WHITE else ContextCompat.getColor(context, R.color.labelPrimary)
        val visible = (0 until bitmap.height).flatMap { y -> (0 until bitmap.width).map { x -> bitmap.getPixel(x, y) } }
            .filter { Color.alpha(it) == 255 }
        assertThat(visible).isNotEmpty
        assertThat(visible.map { it and 0xffffff }.distinct()).containsOnly(expected and 0xffffff)
    }

    @Test fun `unbacked walking template follows current theme and circle glyph remains white`() {
        val segment = TripSegment().apply { modeInfo = ModeInfo().apply { localIconName = "walk" }; transportModeId = TransportMode.ID_WALK }
        assertIcon(segment, false); assertIcon(segment, true); assertIcon(segment, false, true)
    }

    @Test fun `departure marker follows current theme`() {
        val segment = TripSegment().apply { setType(SegmentType.DEPARTURE); transportModeId = TransportMode.ID_WALK }
        assertIcon(segment, false); assertIcon(segment, true)
    }

    @Test fun `persisted segment without mode metadata keeps a visible transport fallback`() {
        assertIcon(TripSegment().apply { transportModeId = TransportMode.ID_WALK }, true)
    }
}
