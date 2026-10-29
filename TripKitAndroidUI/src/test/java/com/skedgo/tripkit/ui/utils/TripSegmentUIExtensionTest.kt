package com.skedgo.tripkit.ui.utils

import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.tripDrawableBitmap
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
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
class TripSegmentUIExtensionTest {
    private fun assertIcon(segment: TripSegment, expectedResource: Int) {
        val context = RuntimeEnvironment.getApplication()
        val observer = segment.getSegmentIconObservable(context, GetTransportIconTintStrategy(context.resources)).test()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        observer.assertNoErrors().assertComplete().assertValueCount(1)
        val icon = observer.values().single()!!
        val expected = ContextCompat.getDrawable(context, expectedResource)!!
        val strategy = GetTransportIconTintStrategy(context.resources).invoke().blockingGet()
        val tinted = strategy.apply(false, false, segment.serviceColor, expected)
        assertThat(icon.tripDrawableBitmap().sameAs(tinted.tripDrawableBitmap())).isTrue()
    }

    @Test
    fun `restored moving segment with unknown local icon retains mode glyph and trip information`() {
        val segment = Gson().fromJson("""{"modeInfo":{"localIcon":"retired-icon","description":"Walk"},"modeIdentifier":"wa_wal","startTime":100,"endTime":200}""", TripSegment::class.java)
        segment.transportModeId = TransportMode.ID_WALK
        val modeInfo = segment.modeInfo
        assertIcon(segment, com.skedgo.tripkit.common.R.drawable.ic_walk)
        assertThat(segment.modeInfo).isSameAs(modeInfo)
        assertThat(segment.modeInfo!!.description).isEqualTo("Walk")
    }

    @Test
    fun `segment with missing optional mode metadata renders its known transport mode`() {
        assertIcon(TripSegment().apply { transportModeId = TransportMode.ID_BICYCLE }, com.skedgo.tripkit.common.R.drawable.ic_bicycle)
    }

    @Test
    fun `unknown moving mode renders generic transport rather than an empty summary`() {
        assertIcon(TripSegment().apply { transportModeId = "new-mode" }, com.skedgo.tripkit.common.R.drawable.ic_public_transport)
    }
    @Test
    fun `departure without mode data uses the existing location marker`() {
        assertIcon(TripSegment().apply { setType(com.skedgo.tripkit.routing.SegmentType.DEPARTURE) }, R.drawable.v4_ic_map_location)
    }

    @Test
    fun `recognised local icon keeps its original glyph`() {
        val segment = TripSegment().apply {
            modeInfo = com.skedgo.tripkit.routing.ModeInfo().apply { localIconName = "bus" }
        }
        assertIcon(segment, com.skedgo.tripkit.common.R.drawable.ic_bus)
    }

}
