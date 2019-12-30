package com.skedgo.tripkit.analytics

import com.skedgo.tripkit.ui.personaldata.MyPersonalDataRepository
import io.reactivex.Observable
import io.reactivex.functions.Function3
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class ReportPlannedTrip @Inject constructor(private val myPersonalDataRepository: MyPersonalDataRepository) {
  open fun execute(
      selectedTrip: Observable<Trip>,
      getVisibleTripGroups: () -> Observable<List<TripGroup>>,
      getSource: Observable<TripSource>,
      getChoiceSet: (Trip, List<TripGroup>) -> List<Choice>,
      userInfoRepository: UserInfoRepository
  ): Observable<Unit> {
    return myPersonalDataRepository.isUploadTripSelectionEnabled()
        .flatMapObservable {
          if (it) {
            selectedTrip.debounce(5, TimeUnit.SECONDS)
                .filter { it.plannedURL != null }
                .withLatestFrom(getVisibleTripGroups(), getSource.first(TripSource.Unknown).toObservable(),
                        Function3<Trip, List<TripGroup>, TripSource, Triple<Trip, TripSource, List<Choice>>>
                        { selectedTrip, visibleTripGroups, source ->
                          Triple(selectedTrip, source, getChoiceSet(selectedTrip, visibleTripGroups))
                })
                .flatMap { userInfoRepository.setUserInfo(it.first.plannedURL!!, UserInfo(it.second, it.third)) }
          } else {
            Observable.empty()
          }
        }
  }
}
