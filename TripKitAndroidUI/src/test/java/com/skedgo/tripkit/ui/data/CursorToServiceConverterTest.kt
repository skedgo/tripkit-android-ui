package com.skedgo.tripkit.ui.data

import android.database.Cursor
import com.google.gson.Gson
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
class CursorToServiceConverterTest {

    private lateinit var converter: CursorToServiceConverter
    private lateinit var mockCursor: Cursor
    private val gson = Gson()

    @Before
    fun setUp() {
        mockCursor = mockk(relaxed = true) // Use relaxed mode to avoid missing stubs
        converter = CursorToServiceConverter(gson)

        // Mock column indices
        every { mockCursor.getColumnIndex(DbFields.ID.name) } returns 0
        every { mockCursor.getColumnIndex(DbFields.PAIR_IDENTIFIER.name) } returns 1
        every { mockCursor.getColumnIndex(DbFields.STOP_CODE.name) } returns 2
        every { mockCursor.getColumnIndex(DbFields.END_STOP_CODE.name) } returns 3
        every { mockCursor.getColumnIndex(DbFields.MODE.name) } returns 4
        every { mockCursor.getColumnIndex(DbFields.START_TIME.name) } returns 5
        every { mockCursor.getColumnIndex(DbFields.END_TIME.name) } returns 6
        every { mockCursor.getColumnIndex(DbFields.FREQUENCY.name) } returns 7
        every { mockCursor.getColumnIndex(DbFields.SERVICE_NUMBER.name) } returns 8
        every { mockCursor.getColumnIndex(DbFields.SERVICE_NAME.name) } returns 9
        every { mockCursor.getColumnIndex(DbFields.SERVICE_TRIP_ID.name) } returns 10
        every { mockCursor.getColumnIndex(DbFields.SERVICE_COLOR_RED.name) } returns 11
        every { mockCursor.getColumnIndex(DbFields.SERVICE_COLOR_GREEN.name) } returns 12
        every { mockCursor.getColumnIndex(DbFields.SERVICE_COLOR_BLUE.name) } returns 13
        every { mockCursor.getColumnIndex("realTimeDeparture") } returns 14
        every { mockCursor.getColumnIndex("realTimeArrival") } returns 15
        every { mockCursor.getColumnIndex(DbFields.FAVOURITE.name) } returns 16
        every { mockCursor.getColumnIndex(DbFields.SEARCH_STRING.name) } returns 17
        every { mockCursor.getColumnIndex(DbFields.SERVICE_TIME.name) } returns 18
        every { mockCursor.getColumnIndex(DbFields.WHEELCHAIR_ACCESSIBLE.name) } returns 19
        every { mockCursor.getColumnIndex(DbFields.BICYCLE_ACCESSIBLE.name) } returns 20
        every { mockCursor.getColumnIndex(DbFields.START_STOP_SHORT_NAME.name) } returns 21
        every { mockCursor.getColumnIndex(DbFields.MODE_INFO.name) } returns 22
        every { mockCursor.getColumnIndex(DbFields.SERVICE_DIRECTION.name) } returns 23
        every { mockCursor.getColumnIndex(DbFields.START_PLATFORM.name) } returns 24

        // Mock data retrieval from the cursor
        every { mockCursor.getInt(0) } returns 12345
        every { mockCursor.getString(1) } returns "Pair_ABC"
        every { mockCursor.getString(2) } returns "Stop_001"
        every { mockCursor.getString(3) } returns "Stop_999"
        every { mockCursor.getString(4) } returns "BUS"
        every { mockCursor.getLong(5) } returns 1617187200L
        every { mockCursor.getLong(6) } returns 1617187500L
        every { mockCursor.getInt(7) } returns 10
        every { mockCursor.getString(8) } returns "123"
        every { mockCursor.getString(9) } returns "City Express"
        every { mockCursor.getString(10) } returns "Trip_789"
        every { mockCursor.getInt(11) } returns 255
        every { mockCursor.getInt(12) } returns 0
        every { mockCursor.getInt(13) } returns 0
        every { mockCursor.getInt(14) } returns 1617187250
        every { mockCursor.getInt(15) } returns 1617187450
        every { mockCursor.getInt(16) } returns 1
        every { mockCursor.getString(17) } returns "search_text"
        every { mockCursor.getLong(18) } returns 1617187200L
        every { mockCursor.getInt(19) } returns 1
        every { mockCursor.getInt(20) } returns 0
        every { mockCursor.getString(21) } returns "ShortStop"
        every { mockCursor.getString(22) } returns """{"type":"bus","name":"Express"}"""
        every { mockCursor.getString(23) } returns "Northbound"
        every { mockCursor.getString(24) } returns "Platform 3"
    }

    @Test
    fun `apply should convert cursor to TimetableEntry correctly`() {
        // Act
        val result = converter.apply(mockCursor)

        // Assert
        assertNotNull(result)
        assertEquals(12345L, result.id)
        assertEquals("Pair_ABC", result.pairIdentifier)
        assertEquals("Stop_001", result.stopCode)
        assertEquals("Stop_999", result.endStopCode)
        assertEquals("BUS", result.mode?.name)
        assertEquals(1617187200L, result.startTimeInSecs)
        assertEquals(1617187500L, result.endTimeInSecs)
        assertEquals(10, result.frequency)
        assertEquals("123", result.serviceNumber)
        assertEquals("City Express", result.serviceName)
        assertEquals("Trip_789", result.serviceTripId)
        assertEquals(255, result.serviceColor?.red)
        assertEquals(0, result.serviceColor?.green)
        assertEquals(0, result.serviceColor?.blue)
        assertEquals(1617187250, result.realTimeDeparture)
        assertEquals(1617187450, result.realTimeArrival)
        assertEquals(true, result.isFavourite)
        assertEquals("search_text", result.searchString)
        assertEquals(1617187200L, result.serviceTime)
        assertEquals(true, result.wheelchairAccessible)
        assertEquals(false, result.bicycleAccessible)
        assertEquals("ShortStop", result.startStopShortName)
        assertEquals("Northbound", result.serviceDirection)
        assertEquals("Platform 3", result.startPlatform)

        // Verify that getColumnIndex was called
        verify { mockCursor.getColumnIndex(any()) }

        // Verify that the required fields were fetched
        verify { mockCursor.getString(any()) }
        verify { mockCursor.getInt(any()) }
        verify { mockCursor.getLong(any()) }
    }
}
