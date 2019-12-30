package com.skedgo.tripkit.ui.servicedetail
import android.util.Pair
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.ServiceRepository
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import io.reactivex.Single
import javax.inject.Inject

class FetchAndLoadServices @Inject constructor(
    private val serviceRepository: ServiceRepository) {
  fun execute(service: TimetableEntry, stop: ScheduledStop): Single<Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>>> {
    return serviceRepository.fetchServices(service, stop)
        .andThen(serviceRepository.loadServices(service, stop))
  }
}