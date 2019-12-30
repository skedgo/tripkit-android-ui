package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.ServiceStopsProvider
import com.skedgo.tripkit.ui.timetables.ServiceRepository
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

typealias ServiceStopAndLine = android.util.Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>>

class LoadServices @Inject constructor(
        private val context: Context,
        private val serviceRepository: ServiceRepository) {

    fun fetch(service: TimetableEntry, stop: ScheduledStop): Completable {
        return serviceRepository.fetchServices(service, stop)
    }
  fun execute(service: TimetableEntry, stop: ScheduledStop): Flowable<ServiceStopAndLine> {
      return Flowable.create(FlowableOnSubscribe<Single<ServiceStopAndLine>>()  {
            val observer = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
              it.onNext(serviceRepository.loadServices(service, stop))
            }
          }
          it.onNext(serviceRepository.loadServices(service, stop))
          context.contentResolver.registerContentObserver(ServiceStopsProvider.STOPS_BY_SERVICE_URI, false, observer)
          it.setCancellable() { context.contentResolver.unregisterContentObserver(observer) }
      }, BackpressureStrategy.LATEST)
              .observeOn(Schedulers.io())
              .flatMapSingle { it }
  }
}