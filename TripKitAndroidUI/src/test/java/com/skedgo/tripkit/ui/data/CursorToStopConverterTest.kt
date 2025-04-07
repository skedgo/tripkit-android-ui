package com.skedgo.tripkit.ui.data

import android.database.Cursor
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.data.database.DbFields
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CursorToStopConverterTest {

    private lateinit var converter: CursorToStopConverter
    private lateinit var mockCursor: Cursor
    private val gson = Gson()

    @Before
    fun setUp() {
        mockCursor = mockk(relaxed = true) // Use relaxed mode to avoid missing stubs
        converter = CursorToStopConverter(gson)

        // Mock column indices
        every { mockCursor.getColumnIndex(DbFields.CODE.name) } returns 0
        every { mockCursor.getColumnIndex(DbFields.ID.name) } returns 1
        every { mockCursor.getColumnIndex("stop_id") } returns 2
        every { mockCursor.getColumnIndex(DbFields.SHORT_NAME.name) } returns 3
        every { mockCursor.getColumnIndex(DbFields.LAT.name) } returns 4
        every { mockCursor.getColumnIndex(DbFields.LON.name) } returns 5
        every { mockCursor.getColumnIndex(DbFields.FAVOURITE.name) } returns 6
        every { mockCursor.getColumnIndex(DbFields.NAME.name) } returns 7
        every { mockCursor.getColumnIndex(DbFields.ADDRESS.name) } returns 8
        every { mockCursor.getColumnIndex(DbFields.SERVICES.name) } returns 9
        every { mockCursor.getColumnIndex(DbFields.STOP_TYPE.name) } returns 10
        every { mockCursor.getColumnIndex(DbFields.MODE_INFO.name) } returns 11
        every { mockCursor.getColumnIndex(DbFields.FILTER.name) } returns 12
        every { mockCursor.getColumnIndex(DbFields.FAVOURITE_SORT_ORDER_POSITION.name) } returns 13

        // Mock data retrieval from the cursor
        every { mockCursor.getString(0) } returns "Stop_001"
        every { mockCursor.getLong(1) } returns 1001L
        every { mockCursor.getLong(2) } returns 2001L
        every { mockCursor.getString(3) } returns "Short Stop"
        every { mockCursor.getDouble(4) } returns -33.865143
        every { mockCursor.getDouble(5) } returns 151.209900
        every { mockCursor.getInt(6) } returns 1
        every { mockCursor.getString(7) } returns "Main Station"
        every { mockCursor.getString(8) } returns "123 Main St, Sydney"
        every { mockCursor.getString(9) } returns "Bus,Train"
        every { mockCursor.getString(10) } returns "BUS"
        every { mockCursor.getString(11) } returns """{"type":"bus","name":"Express"}"""
        every { mockCursor.getString(12) } returns "default"
        every { mockCursor.getInt(13) } returns 5
    }

    @Test
    fun `apply should convert cursor to ScheduledStop correctly`() {
        // Act
        val result = converter.apply(mockCursor)

        // Assert
        assertNotNull(result)
        assertEquals("Stop_001", result.code)
        assertEquals(1001L, result.mId)
        assertEquals(2001L, result.stopId)
        assertEquals("Short Stop", result.shortName)
        assertEquals(-33.865143, result.lat, 1e-6)
        assertEquals(151.209900, result.lon, 1e-6)
        assertEquals(true, result.isFavourite)
        assertEquals("Main Station", result.name)
        assertEquals("123 Main St, Sydney", result.address)
        assertEquals("Bus,Train", result.services)
        assertEquals(StopType.BUS, result.type)
        assertEquals("default", result.currentFilter)
        assertEquals(5, result.favouriteSortOrderIndex)

        // Verify that getColumnIndex was called
        verify { mockCursor.getColumnIndex(any()) }

        // Verify that the required fields were fetched
        verify { mockCursor.getString(any()) }
        verify { mockCursor.getInt(any()) }
        verify { mockCursor.getLong(any()) }
        verify { mockCursor.getDouble(any()) }
    }
}
