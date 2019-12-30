package com.skedgo.tripkit.ui.routingresults
import io.reactivex.Observable
import javax.inject.Inject

open class WhenToRefreshRoutingResults @Inject internal constructor(
    private val isModeIncludedInTripsRepository: IsModeIncludedInTripsRepository,
    private val isModeMinimizedRepository: IsModeMinimizedRepository
) {
  open fun execute(): Observable<Unit> =
      Observable
          .merge(
              isModeIncludedInTripsRepository.onChanged(),
              isModeMinimizedRepository.onChanged()
          )
          .map { Unit }
}
