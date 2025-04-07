package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.database.timetables.ServiceAlertMapper
import com.skedgo.tripkit.data.database.timetables.ServiceAlertsDao
import com.skedgo.tripkit.ui.data.CursorToServiceConverter
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.TimetableProvider
import com.skedgo.tripkit.ui.utils.Optional
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.Test
import skedgo.tripgo.data.timetables.ParentStopDao

class FetchAndLoadTimetableTest {

    private lateinit var fetchAndLoadTimetable: FetchAndLoadTimetable
    private val converter: CursorToServiceConverter = mockk()
    private val parentStopDao: ParentStopDao = mockk()
    private val serviceAlertsDao: ServiceAlertsDao = mockk()
    private val serviceAlertsMapper: ServiceAlertMapper = mockk()
    private val context: Context = mockk(relaxed = true)
    private val fetchTimetable: FetchTimetable = mockk()
    private val cursor: Cursor = mockk(relaxed = true)

    private val embarkationStopCodes = listOf("StopA")
    private val disembarkationStopCodes = listOf("StopB")
    private val region: Region = mockk()
    private val startTimeInSecs = 1234567890L

    private val expectedTimetableEntries = listOf(mockk<TimetableEntry>(relaxed = true))
    private val expectedParentStop: Optional<ScheduledStop> = Optional(mockk {
        every { code } returns "StopA"
    })

    @Before
    fun setup() {
        fetchAndLoadTimetable = FetchAndLoadTimetable(
            converter, parentStopDao, serviceAlertsDao,
            serviceAlertsMapper, context, fetchTimetable
        )

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        TimetableProvider.SCHEDULED_SERVICES_URI = Uri.parse("content://com.skedgo.tripkit.timetable/scheduled_services")

        every { fetchTimetable.execute(any(), any(), any(), any()) } returns
            Single.just(Pair(expectedTimetableEntries, expectedParentStop))

        every { parentStopDao.getChildrenStops(any()) } returns
            Observable.just(listOf(mockk { every { childrenStopCode } returns "StopA" }))

        every { context.contentResolver.query(any(), any(), any(), any(), any()) } returns mockk {
            every { count } returns 1
            every { moveToPosition(any()) } returns true
            every { close() } just Runs
        }

        every { expectedTimetableEntries[0].alerts = any() } just Runs

        every { serviceAlertsDao.getAlertForService(any()) } returns Single.just(emptyList())

        every { converter.apply(any()) } returns mockk(relaxed = true)
    }

    @Test
    fun `execute should fetch and load timetable successfully`() {
        val testObserver: TestObserver<Pair<List<TimetableEntry>, Optional<ScheduledStop>>> =
            fetchAndLoadTimetable.execute(embarkationStopCodes, disembarkationStopCodes, region, startTimeInSecs)
                .test()

        // Ensure at least one value is emitted
        assert(testObserver.values().isNotEmpty()) { "Expected at least one value, but got none!" }

        // Validate emitted result
        testObserver.assertComplete()
        assertEquals(true, expectedTimetableEntries.isNotEmpty())
    }
}