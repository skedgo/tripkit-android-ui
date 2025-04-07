package com.skedgo.tripkit.ui.timetables

import android.text.Html
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GetDirectionTextTest {

    private lateinit var getDirectionText: GetDirectionText

    @Before
    fun setUp() {
        getDirectionText = GetDirectionText()

        // Mock Html.fromHtml() to return expected string
        mockkStatic(Html::class)
        every { Html.fromHtml(any()).toString() } answers { firstArg() } // Returns the same string
    }

    @Test
    fun `should return service direction if available`() {
        // Mock TimetableEntry
        val service = mockk<TimetableEntry> {
            every { serviceDirection } returns "Northbound"
            every { startStop } returns null
        }

        // Run function
        val result = getDirectionText.execute(service)

        // Verify
        assertEquals("Northbound", result)
    }

    @Test
    fun `should return start stop name if service direction is empty`() {
        // Mock TimetableEntry
        val service = mockk<TimetableEntry> {
            every { serviceDirection } returns null
            every { startStop } returns mockk<ScheduledStop> {
                every { name } returns "Central Station"
                every { shortName } returns null
            }
        }

        // Run function
        val result = getDirectionText.execute(service)

        // Verify
        assertEquals("Central Station", result)
    }

    @Test
    fun `should return empty string if both service direction and start stop name are empty`() {
        // Mock TimetableEntry
        val service = mockk<TimetableEntry> {
            every { serviceDirection } returns null
            every { startStop } returns mockk<ScheduledStop> {
                every { name } returns null
                every { shortName } returns null
            }
        }

        // Run function
        val result = getDirectionText.execute(service)

        // Verify
        assertEquals("", result)
    }

    @Test
    fun `should return formatted string with short name and direction`() {
        val service = mockk<TimetableEntry> {
            every { serviceDirection } returns "Northbound"
            every { startStop } returns mockk<ScheduledStop> {
                every { name } returns "Central Station"
                every { shortName } returns "CEN"
            }
        }

        // Mock Html.fromHtml behavior
        every { Html.fromHtml("CEN &middot; Northbound").toString() } returns "CEN · Northbound"

        val result = getDirectionText.execute(service)

        assertEquals("CEN · Northbound", result)
    }
}
