package com.skedgo.tripkit.ui.timetables

import android.util.Pair
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import io.reactivex.Completable
import io.reactivex.Single

interface ServiceRepository {
  fun fetchServices(service: TimetableEntry, stop: ScheduledStop): Completable

  fun loadServices(service: TimetableEntry, stop: ScheduledStop): Single<Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>>>
}