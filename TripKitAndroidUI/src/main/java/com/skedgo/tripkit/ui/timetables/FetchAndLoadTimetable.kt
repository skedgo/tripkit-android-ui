package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.database.Cursor
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.data.database.timetables.ServiceAlertMapper
import com.skedgo.tripkit.data.database.timetables.ServiceAlertsDao
import com.skedgo.tripkit.ui.data.CursorToServiceConverter
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.TimetableProvider
import com.skedgo.tripkit.ui.timetables.domain.TimetableLoaderFactory
import com.skedgo.tripkit.ui.utils.Optional
import io.reactivex.Observable
import io.reactivex.Single
import skedgo.tripgo.data.timetables.ParentStopDao
import javax.inject.Inject

open class FetchAndLoadTimetable @Inject constructor(
    private val converter: CursorToServiceConverter,
    private val parentStopDao: ParentStopDao,
    private val serviceAlertsDao: ServiceAlertsDao,
    private val serviceAlertsMapper: ServiceAlertMapper,
    private val context: Context,
    private val fetchTimetable: FetchTimetable
) {

    open fun execute(
        embarkationStopCodes: List<String>,
        disembarkationStopCodes: List<String>?,
        region: Region,
        startTimeInSecs: Long
    ): Single<Pair<List<TimetableEntry>, Optional<ScheduledStop>>> {

        return fetchTimetable.execute(
            embarkationStopCodes,
            disembarkationStopCodes,
            region,
            startTimeInSecs
        )
            .flatMap { (_, parentStop) ->
                loadTimetable(embarkationStopCodes, startTimeInSecs)
                    .map { it to parentStop }
            }
    }

    private fun loadTimetable(
        embarkationStopCodes: List<String>,
        startTimeInSecs: Long
    ): Single<List<TimetableEntry>> {

        return Observable.fromIterable(embarkationStopCodes)
            .flatMap { parentStopCode ->
                parentStopDao.getChildrenStops(parentStopCode)
                    .take(1)
                    .defaultIfEmpty(emptyList())
                    .map { it.map { it.childrenStopCode }.plus(parentStopCode) }
                    .flatMap { Observable.fromIterable(it) }
            }
            .toList()
            .map { it.toSet().toList() }
            .flatMap {
                Single.fromCallable {
                    val params = TimetableLoaderFactory.buildQueryParams(it, startTimeInSecs)
                    val cursor: Cursor = context.contentResolver.query(
                        TimetableProvider.SCHEDULED_SERVICES_URI,
                        null,
                        params.selection,
                        params.selectionArgs,
                        params.sortOrder
                    )!!
                    val services = mutableListOf<TimetableEntry>()
                    for (i in 0 until cursor.count) {
                        cursor.moveToPosition(i)
                        services.add(converter.apply(cursor))
                    }

                    cursor.close()
                    services
                }
            }
            .flatMap {
                Observable.fromIterable(it)
                    .flatMapSingle { service ->
                        serviceAlertsDao.getAlertForService(service.serviceTripId)
                            .map { it.map { serviceAlertsMapper.toModel(it) } }
                            .map {
                                service.alerts = ArrayList(it)
                                service
                            }
                    }
                    .toList()
            }
    }
}