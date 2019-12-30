package com.skedgo.tripkit.ui.tripresult
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.ui.routingresults.FetchingRealtimeStatusRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import com.skedgo.tripkit.logging.ErrorLogger

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

open class UpdateTripForRealtime @Inject internal constructor(
        private val tripGroupRepository: TripGroupRepository,
        private val errorLogger: ErrorLogger,
        private val fetchingRealtimeStatusRepository: FetchingRealtimeStatusRepository,
        private val tripUpdater: TripUpdater
) {
  private val subscriptions: CompositeDisposable = CompositeDisposable()

  fun start(getTripGroup: Observable<TripGroup>) {
    Observable.interval(0, 10, TimeUnit.SECONDS, Schedulers.computation())
        .withLatestFrom(getTripGroup, BiFunction<Long, TripGroup, TripGroup>{ _, tripGroup -> tripGroup } )
        .filter { it.displayTrip!!.hasQuickBooking().not() }
        .flatMap { startAsync(it) }
        .subscribe({
          tripGroupRepository.updateTrip(it.second.uuid(), it.second.displayTrip!!.uuid(), it.first)
              .subscribe({}, errorLogger::trackError)
        }, errorLogger::logError)
        .run {
          subscriptions.add(this)
        }
  }

  fun startForBooking(getTripGroup: Observable<TripGroup>) {
    Observable.interval(0, 10, TimeUnit.SECONDS, Schedulers.computation())
        .withLatestFrom(getTripGroup, BiFunction<Long, TripGroup, TripGroup>{ _, tripGroup -> tripGroup } )
        .flatMap { startAsync(it) }
        .subscribe({
          tripGroupRepository.updateTrip(it.second.uuid(), it.second.displayTrip!!.uuid(), it.first)
              .subscribe({}, errorLogger::trackError)
        }, errorLogger::logError)
        .run {
          subscriptions.add(this)
        }
  }

  private fun startAsync(group: TripGroup): Observable<Pair<Trip, TripGroup>> {
    return Observable.just(group)
        .observeOn(Schedulers.io())
        .map { group.displayTrip!!.updateURL }
        .compose { updateUrlStream ->
          val nextUrlToFetch = AtomicReference<String>()
          updateUrlStream
              .flatMap { updateUrl ->
                val url = nextUrlToFetch.get() ?: updateUrl
                if (url != null) {
                  // FIXME: Reduce side effects.
                  tripUpdater.getUpdateAsync(url)
                      .onErrorResumeNext(Observable.empty())
                      .doOnSubscribe {
                        fetchingRealtimeStatusRepository.put(group.uuid(), true)
                      }
                      .doFinally {
                        fetchingRealtimeStatusRepository.put(group.uuid(), false)
                      }
                } else {
                  Observable.empty()
                }
              }
              .doOnNext { trip -> nextUrlToFetch.set(trip.updateURL) }
        }
        .map { trip -> Pair(trip, group) }
  }

  fun stop() {
    subscriptions.clear()
  }
}