package com.skedgo.tripkit.ui.tripresult

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Redmine #19351: a preview restored after rotation / font-size change must keep the selected
 * trip. Parsed trips have tripId 0 and a fresh random uuid, so the server URL identifies them.
 */
class TripSelectionTest {

    private val first = Trip().apply { saveURL = "https://example.test/trip/first" }
    private val selected = Trip().apply { saveURL = "https://example.test/trip/selected" }
    private val group = TripGroup().apply { uuid("group"); addTrip(first); addTrip(selected) }

    @Test
    fun `stable key finds the selected trip when uuid is stale and tripId is zero`() {
        val found = group.findSelectedTrip(
            uuid = "uuid-from-before-recreation",
            tripId = 0L,
            stableKey = "https://example.test/trip/selected"
        )

        assertSame(selected, found)
    }

    @Test
    fun `uuid match wins when it is still valid`() {
        assertSame(selected, group.findSelectedTrip(selected.uuid, null, null))
    }

    @Test
    fun `falls back to the display trip when nothing matches`() {
        assertSame(group.displayTrip, group.findSelectedTrip("unknown", 0L, "unknown"))
    }

    @Test
    fun `stable key prefers saveURL then temporaryURL then updateURL`() {
        assertEquals("s", Trip().apply { saveURL = "s"; temporaryURL = "t"; updateURL = "u" }.stableKey)
        assertEquals("t", Trip().apply { temporaryURL = "t"; updateURL = "u" }.stableKey)
        assertEquals("u", Trip().apply { updateURL = "u" }.stableKey)
    }
}
