package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.util.Pair
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.ServiceStopsProvider
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class ServiceRepositoryImpl @Inject constructor(
        private val context: Context,
        private val fetchService: FetchService) : ServiceRepository {

  override fun fetchServices(service: TimetableEntry, stop: ScheduledStop): Completable {
    return fetchService.execute(service, stop)
  }

  override fun loadServices(service: TimetableEntry, stop: ScheduledStop): Single<Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>>> {
    val timeInSeconds: Long = service.startTimeInSecs
    val serviceTripId = service.serviceTripId
    return Single
        .fromCallable {
          val cursor = context.contentResolver.query(
              ServiceStopsProvider.STOPS_BY_SERVICE_URI,
              ServiceStopsLoaderFactory.PROJECTION,
              ServiceStopsLoaderFactory.SELECTION,
              arrayOf(serviceTripId, TimeUtils.getJulianDay(timeInSeconds * TimeUtils.InMillis.SECOND).toString()),
              ServiceStopsLoaderFactory.SORT_ORDER)
          val data = LoadServiceTask(stop, cursor).call()
          cursor?.close()
          data
        }
  }

}