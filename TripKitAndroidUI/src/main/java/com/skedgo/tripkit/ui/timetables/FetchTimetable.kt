package com.skedgo.tripkit.ui.timetables

import android.content.Context
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.data.database.timetables.ServiceAlertMapper
import com.skedgo.tripkit.data.database.timetables.ServiceAlertsDao
import com.skedgo.tripkit.ui.model.DeparturesResponse
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.TimetableProvider
import com.skedgo.tripkit.ui.realtime.RealtimeAlertRepository
import com.skedgo.tripkit.ui.timetables.domain.DeparturesRepository
import com.skedgo.tripkit.ui.utils.Optional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import skedgo.tripgo.data.timetables.ParentStopDao
import skedgo.tripgo.data.timetables.ParentStopEntity
import javax.inject.Inject

open class FetchTimetable @Inject constructor(
        private val departuresRepository: DeparturesRepository,
        private val realtimeAlertRepository: RealtimeAlertRepository,
        private val parentStopDao: ParentStopDao,
        private val timetableEntriesMapper: TimetableEntriesMapper,
        private val serviceAlertMapper: ServiceAlertMapper,
        private val serviceAlertsDao: ServiceAlertsDao,
        private val context: Context
) {

  open fun execute(embarkationStopCodes: List<String>,
                   region: Region,
                   startTimeInSecs: Long
  ): Single<Pair<List<TimetableEntry>, Optional<ScheduledStop>>> =
      departuresRepository.getTimetableEntries(
          region.name!!,
          embarkationStopCodes,
          null,
          startTimeInSecs)
          .map { response ->
            if (response == null) {
              error(RuntimeException("Failed to fetch timetable"))
            }

            // Save alerts
            if (!response.alerts.isNullOrEmpty()) {
                realtimeAlertRepository.addAlerts(response.alerts!!)
            }

            // Add real time alerts
            response.serviceList.orEmpty().forEach { service ->
              service.alertHashCodes?.let {
                realtimeAlertRepository.addAlertHashCodesForId(service.serviceTripId, it.toList())
              }
            }

            // set start stop from parent info
            response.serviceList.orEmpty().forEach { service ->
              service.startStop = response.parentInfo?.children?.find { it.code == service.stopCode }
            }

            // TODO: remove when refactoring TimetablePager
            response.parentInfo?.let { parent ->
              parentStopDao.insert(parent.children.map { ParentStopEntity(parent.code, it.code) })

              // save alerts
              parent.alertHashCodes?.let {
                realtimeAlertRepository.addAlertHashCodesForId(parent.code, it)
              }
            }

            // TODO: remove when refactoring TimetablePager
            val serviceValuesList = timetableEntriesMapper.toContentValues(response.serviceList.orEmpty())
            context.contentResolver.bulkInsert(TimetableProvider.SCHEDULED_SERVICES_URI, serviceValuesList)
            saveAlerts(response)

            response.serviceList.orEmpty() to Optional<ScheduledStop>(response.parentInfo)
          }
          .map {
            it.first.forEach { timetable ->
              val savedAlerts = realtimeAlertRepository.getAlerts(timetable.serviceTripId)
                timetable.alerts = ArrayList(savedAlerts.orEmpty())
            }
            it
          }

  private fun saveAlerts(response: DeparturesResponse) {
      val alertHashCodesToAlerts = response.alerts.orEmpty()
        .map { it.remoteHashCode() to it }
        .toMap()

    Observable.fromIterable(response.serviceList.orEmpty())
        .flatMapSingle { service ->
          serviceAlertsDao.getAlertForService(serviceId = service.serviceTripId)
              .flatMapCompletable {
                Completable.fromAction {
                  serviceAlertsDao.deleteAlertByService(it)
                }
              }
              .andThen(Single.just(service))
              .map {
                it.alertHashCodes.orEmpty()
                    .map {
                      serviceAlertMapper.toEntity(service.serviceTripId, alertHashCodesToAlerts[it]!!)
                    }
              }
        }
        .flatMapCompletable {
          Completable.fromAction {
              serviceAlertsDao.insertAlerts(it)
          }
        }
        .subscribe()
  }
}