package com.skedgo.tripkit.ui.trippreview.v2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.common.model.booking.ImmutableBooking
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class TripPreviewSegmentStateTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val state = TripPreviewSegmentState(context)
    private val key = UUID.randomUUID().toString()

    @Test
    fun `snapshot restores selected segment and all transient trip relationships`() {
        val other = TripSegment().apply { id = "other"; segmentId = 101 }
        val selected = TripSegment().apply {
            id = "selected"; segmentId = 102
            booking = ImmutableBooking.builder().quickBookingsUrl("https://example.test/booking").build()
        }
        val trip = Trip().apply {
            uuid = "selected-trip"
            temporaryURL = "https://example.test/selected-trip"
            segmentList = mutableListOf(other, selected)
        }
        val alternative = Trip().apply { uuid = "alternative-trip" }
        TripGroup().apply { uuid("group"); addTrip(alternative); addTrip(trip) }
        assertThat(state.save(key, selected)).isTrue()

        val restored = TripPreviewSegmentState(context).restore(key)!!
        assertThat(restored).isNotSameAs(selected)
        assertThat(restored.id).isEqualTo("selected")
        assertThat(restored.segmentId).isEqualTo(102)
        assertThat(restored.booking!!.quickBookingsUrl).isEqualTo("https://example.test/booking")
        assertThat(restored.trip!!.uuid).isEqualTo("selected-trip")
        assertThat(restored.trip!!.temporaryURL).isEqualTo("https://example.test/selected-trip")
        assertThat(restored.trip!!.segmentList[1]).isSameAs(restored)
        assertThat(restored.trip!!.segmentList[0].trip).isSameAs(restored.trip)
        assertThat(restored.trip!!.group!!.uuid()).isEqualTo("group")
        assertThat(restored.trip!!.group!!.trips!![1]).isSameAs(restored.trip)
    }

    @Test
    fun `trip without group remains without group`() {
        val selected = TripSegment().apply { id = "selected" }
        Trip().apply { segmentList = mutableListOf(selected) }
        assertThat(state.save(key, selected)).isTrue()
        assertThat(state.restore(key)!!.trip!!.group).isNull()
    }

    @Test
    fun `standalone caller segment can be recreated without inventing a trip`() {
        assertThat(state.save(key, TripSegment().apply { id = "standalone" })).isTrue()
        val restored = state.restore(key)!!
        assertThat(restored.id).isEqualTo("standalone")
        assertThat(restored.trip).isNull()
    }

    @Test
    fun `missing or evicted cache is unavailable`() {
        assertThat(state.restore(key)).isNull()
        state.save(key, TripSegment())
        state.delete(key)
        assertThat(state.restore(key)).isNull()
    }

    @Test
    fun `corrupted and unsupported snapshots are rejected`() {
        val file = File(context.cacheDir, "trip-preview-segments/$key")
        file.parentFile!!.mkdirs()
        for (data in listOf("{", "null", "{}", "{\"version\":2}", "{\"version\":1,\"segment\":null}")) {
            file.writeText(data)
            assertThat(state.restore(key)).describedAs(data).isNull()
        }
    }

    @Test
    fun `unavailable segment is rejected without selecting another segment`() {
        val selected = TripSegment().apply { id = "missing" }
        val other = TripSegment().apply { id = "other" }
        Trip().apply { segmentList = mutableListOf(other) }.also { selected.trip = it }
        state.save(key, selected)
        assertThat(state.restore(key)).isNull()
    }

    @Test
    fun `snapshots from different dialogs do not overwrite each other`() {
        val otherKey = UUID.randomUUID().toString()
        state.save(key, TripSegment().apply { id = "first" })
        state.save(otherKey, TripSegment().apply { id = "second" })
        assertThat(state.restore(key)!!.id).isEqualTo("first")
        assertThat(state.restore(otherKey)!!.id).isEqualTo("second")
    }

    @Test
    fun `incomplete and mismatched segment data is rejected`() {
        state.save(key, TripSegment().apply { id = "selected" })
        val file = File(context.cacheDir, "trip-preview-segments/$key")
        val data = file.readText()
        file.writeText(data.replace("\"id\":\"selected\"", "\"id\":\"wrong\""))
        assertThat(state.restore(key)).isNull()
        file.writeText("{\"version\":1,\"segment\":{}}")
        assertThat(state.restore(key)).isNull()
    }

    @Test
    fun `expired snapshots are rejected and deleted`() {
        state.save(key, TripSegment().apply { id = "expired" })
        val file = File(context.cacheDir, "trip-preview-segments/$key")
        assertThat(file.setLastModified(System.currentTimeMillis() - 25 * 60 * 60 * 1000L)).isTrue()
        assertThat(state.restore(key)).isNull()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun `saving prunes abandoned expired snapshots but keeps recent dialogs`() {
        state.save(key, TripSegment().apply { id = "expired" })
        val file = File(context.cacheDir, "trip-preview-segments/$key")
        assertThat(file.setLastModified(System.currentTimeMillis() - 25 * 60 * 60 * 1000L)).isTrue()
        val recentKey = UUID.randomUUID().toString()
        state.save(recentKey, TripSegment().apply { id = "recent" })
        assertThat(file.exists()).isFalse()
        assertThat(state.restore(recentKey)!!.id).isEqualTo("recent")
    }

    @Test
    fun `invalid snapshot references are rejected`() {
        assertThat(state.restore("../../other")).isNull()
        assertThat(state.restore("")).isNull()
    }
}
