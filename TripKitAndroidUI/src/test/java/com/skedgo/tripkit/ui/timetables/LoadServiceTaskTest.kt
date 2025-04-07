package com.skedgo.tripkit.ui.timetables

import android.database.Cursor
import android.text.TextUtils
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.database.DbFields
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoadServiceTaskTest {

    private lateinit var loadServiceTask: LoadServiceTask
    private val mockCursor: Cursor = mockk(relaxed = true)
    private val mockStop: ScheduledStop = mockk(relaxed = true)

    @Before
    fun setUp() {

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { (firstArg<String?>()).isNullOrEmpty() }

        val waypointMockId = 123
        val stopCodeMockId = 124
        val stopBearingMockId = 125
        val stopNameMockId = 126
        val stopDepartureSecsMockId = 127
        val stopArrivalSecsMockId = 128

        every { mockCursor.count } returns 1
        every { mockCursor.getColumnIndex(DbFields.ID.name) } returns -1
        every { mockCursor.moveToPosition(any()) } returns true

        every { mockCursor.getColumnIndex(DbFields.STOP_CODE.name) } returns stopCodeMockId
        every { mockCursor.getString(stopCodeMockId) } returns "STOP_123"

        every { mockCursor.getInt(LoadServiceTaskCursorCols.id) } returns 101
        every { mockCursor.getString(LoadServiceTaskCursorCols.address) } returns "123 Main St"

        every { mockCursor.getColumnIndex(DbFields.NAME.name) } returns stopNameMockId
        every { mockCursor.getString(stopNameMockId) } returns "Central Station"

        every { mockCursor.getColumnIndex(DbFields.BEARING.name) } returns stopBearingMockId
        every { mockCursor.getInt(stopBearingMockId) } returns 90

        every { mockCursor.getString(LoadServiceTaskCursorCols.realTimeStatus) } returns "REAL_TIME"

        every { mockCursor.getColumnIndex(DbFields.DEPARTURE_TIME.name) } returns stopDepartureSecsMockId
        every { mockCursor.getLong(stopDepartureSecsMockId) } returns 1617187200L

        every { mockCursor.getColumnIndex(DbFields.ARRIVAL_TIME.name) } returns stopArrivalSecsMockId
        every { mockCursor.getLong(stopArrivalSecsMockId) } returns 1617187500L

        every { mockCursor.getInt(LoadServiceTaskCursorCols.serviceColor) } returns 0xFF0000
        every { mockCursor.getDouble(LoadServiceTaskCursorCols.lat) } returns 37.7749
        every { mockCursor.getDouble(LoadServiceTaskCursorCols.lon) } returns -122.4194
        every { mockCursor.getInt(LoadServiceTaskCursorCols.wheelchairAccessible) } returns 1
        every { mockCursor.getColumnIndex(DbFields.WAYPOINT_ENCODING.name) } returns waypointMockId
        every { mockCursor.getString(waypointMockId) } returns "n~smEk_{y[[jAU{I??Ja@cDsC????{CeD???BmAgB@o@??@@z@yAlIiE???@~DnAjIl@???@tAl@~DfBj@T???@bFVbENnDRfCL??r@DT@lDXbGd@`DQ??@@TBbFEnCCl@@|FFjEB???@bARvFlA|Cn@|A^??x@PzBz@`EH??@@bATlBb@xCp@t@N??@@rAZhCp@|D~@RPdClCNN??@?dApFjAdGDtEDnD@lAHvD??CbB@~BDlI@lBb@nEBp@s@HeAjB??@@wCrE??@@qEfH????{FdJ??@@q@n@mBfB??@?wAfA{FjE??@@a@C{CnC????sJ`H??@?sKfI???@wKnHd@jB`AXRU"

        mockkStatic(PolyUtil::class)
        every { PolyUtil.decode("encoded_waypoints") } returns listOf(LatLng(37.7749, -122.4194))

        LoadServiceTaskCursorCols.init(mockCursor)
        loadServiceTask = LoadServiceTask(mockStop, mockCursor)
    }

    @Test
    fun `should return stop info and service line info`() {
        val result = loadServiceTask.call()

        assert(result.first.isNotEmpty()) { "Stop info list should not be empty" }
        assert(result.second.isNotEmpty()) { "Service line info list should not be empty" }

        val stopInfo = result.first.first()
        assertEquals("STOP_123", stopInfo.stop.code)
        assertEquals(90, stopInfo.stop.bearing)
        assertEquals("Central Station", stopInfo.stop.name)
        assertEquals(1617187200L, stopInfo.stop.departureSecs())
        assertEquals(1617187500L, stopInfo.stop.arrivalTime)

        verify { mockCursor.moveToPosition(any()) }
        verify { mockCursor.getString(LoadServiceTaskCursorCols.stopCode) }
    }
}
