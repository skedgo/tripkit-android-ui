package com.skedgo.tripkit.ui.timetables

import android.content.Context
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GetFrequencyTextTest {

    private lateinit var getFrequencyText: GetFrequencyText
    private val context: Context = mockk()

    @Before
    fun setUp() {
        getFrequencyText = GetFrequencyText(context)

        // Correctly extract values from `invocation.args[1]`
        every { context.getString(R.string._pattern_every__pattern, any(), any()) } answers {
            val argsArray = it.invocation.args[1] as Array<*>
            val first = argsArray[0] as? String ?: ""
            val frequency = argsArray[1] as? String ?: ""
            "Every $frequency at $first"
        }
    }

    @Test
    fun `should return formatted frequency text with service number`() {
        val service = mockk<TimetableEntry> {
            every { serviceNumber } returns "Bus 123"
            every { startStop } returns null
            every { frequency } returns 10
        }

        val expected = "Every 10 min at Bus 123"
        val result = getFrequencyText.execute(service)

        assertEquals(expected, result)
    }

    @Test
    fun `should return formatted frequency text with start stop name when service number is empty`() {
        val service = mockk<TimetableEntry> {
            every { serviceNumber } returns ""
            every { startStop?.name } returns "Central Station"
            every { frequency } returns 15
        }

        val expected = "Every 15 min at Central Station"
        val result = getFrequencyText.execute(service)

        assertEquals(expected, result)
    }

    @Test
    fun `should return formatted frequency text with empty first string when service number and start stop name are empty`() {
        val service = mockk<TimetableEntry> {
            every { serviceNumber } returns ""
            every { startStop?.name } returns ""
            every { frequency } returns 5
        }

        val expected = "Every 5 min at "
        val result = getFrequencyText.execute(service)

        assertEquals(expected, result)
    }
}