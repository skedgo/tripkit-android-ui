package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.analytics.TripSource
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.favorites.GetTripGroupsFromWayPoints
import com.skedgo.tripkit.ui.routing.GetSortedTripGroups
import com.skedgo.tripkit.ui.routingresults.FetchingRealtimeStatusRepository
import com.skedgo.tripkit.ui.routingresults.SelectedTripGroupRepository
import com.skedgo.tripkit.ui.routingresults.TrackViewingTrip
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripprogress.UpdateTripProgressWithUserLocation
import com.skedgo.tripkit.ui.tripresults.PermissiveTransportViewFilter
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

const val ARG_TRIP_GROUP_ID = "tripGroupId"

class TripResultPagerViewModel @Inject internal constructor(
        private val context: Context,
        private val getSortedTripGroups: GetSortedTripGroups,
//        private val reportPlannedTrip: ReportPlannedTrip,
//        private val userInfoRepository: UserInfoRepository,
//        private val getChoiceSet: GetChoiceSet,
        private val trackViewingTrip: TrackViewingTrip,
        private val errorLogger: ErrorLogger,
//        private val eventTracker: EventTracker,
        private val selectedTripGroupRepository: SelectedTripGroupRepository,

        private val updateTripProgress: UpdateTripProgressWithUserLocation,
        private val tripGroupRepository: TripGroupRepository,
        private val fetchingRealtimeStatusRepository: FetchingRealtimeStatusRepository,
        private val schedulers: SchedulerFactory,
        private val getTripGroupsFromWayPoints: GetTripGroupsFromWayPoints
) : RxViewModel() {
    val fetchingRealtimeStatus = ObservableBoolean()
    val selectedTripGroup by lazy {
        tripGroupRepository.getTripGroup(currentTripGroupId.toString())
    }
    val currentPage = ObservableInt()
    val tripGroupsBinding = ObservableField<List<TripGroup>>(emptyList())

    private val tripGroups: BehaviorRelay<List<TripGroup>> = BehaviorRelay.create()
    val tripSource = BehaviorRelay.create<TripSource>()
    val currentTripGroupId = AtomicReference<String?>(null)
    private var updateTripProgressSubscription: Disposable? = null
    private val tripResultTransportViewFilter = PermissiveTransportViewFilter()

    fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.getString(ARG_TRIP_GROUP_ID)?.let {
            setInitialSelectedTripGroupId(it)
        }
    }

    fun onSavedInstanceState(outState: Bundle) {
        if (currentTripGroupId.get() != null) {
            outState.putString(ARG_TRIP_GROUP_ID, currentTripGroupId.get())
        }
    }

    fun getSortedTripGroups(args: PagerFragmentArguments): Observable<Unit> {
        if (args is FromRoutes) {
            return getSortedTripGroups.execute(args.requestId, args.arriveBy, args.sortOrder, tripResultTransportViewFilter)
                    .subscribeOn(Schedulers.io())
                    .doOnNext {
                        tripGroups.accept(it)
                    }
                    .map { Unit }
        } else if (args is SingleTrip) {
            selectedTripGroupRepository.setSelectedTripGroupId(args.tripGroupId)
            return selectedTripGroupRepository.getSelectedTripGroup().map { listOf(it) }
                    .doOnNext { tripGroups.accept(it) }
                    .map { Unit }
        } else if (args is FavoriteTrip) {
            fetchingRealtimeStatus.set(true)
            return getTripGroupsFromWayPoints.execute(args.favoriteTripId)
                    .doOnNext {
                        setInitialSelectedTripGroupId(it.uuid())
                        fetchingRealtimeStatus.set(false)
                    }
                    .map {
                        listOf(it)
                    }
                    .doOnNext { tripGroups.accept(it) }
                    .map { Unit }
        } else {
            throw IllegalArgumentException("Unknown Argument: $args")
        }
    }

    fun observeInitialPage(): Observable<Unit> {
        return Observables.combineLatest(
                tripGroups.firstOrError().toObservable(),
                selectedTripGroup.hide().firstOrError().toObservable()
        )
        { tripGroups: List<TripGroup>, id: TripGroup ->
            tripGroups.indexOfFirst { id.uuid() == it.uuid() }
        }.doOnNext {
            currentPage.set(it)
        }.map { Unit }
    }

    fun observeTripGroups(): Observable<Unit> {
        return tripGroups
                .doOnNext {
                    tripGroupsBinding.set(it)
                }
                .map { Unit }
    }

    fun updateSelectedTripGroup(): Observable<Unit> {
        return currentPage
                .asObservable()
                .skip(1)
                .withLatestFrom(tripGroups.hide(), BiFunction<Int, List<TripGroup>, TripGroup>
                { id, tripGroups -> tripGroups[id] })
                .observeOn(Schedulers.computation())
                .doOnNext {
                    setInitialSelectedTripGroupId(it.uuid())
                }
                .map { Unit }
    }

    fun loadFetchingRealtimeStatus(): Observable<Boolean> {
        return selectedTripGroup
                .switchMap { fetchingRealtimeStatusRepository.get(it.uuid()) }
                .doOnNext { fetchingRealtimeStatus.set(it) }
    }

    fun trackViewingTrip() = trackViewingTrip.execute(tripSource.hide())
//
//  fun reportPlannedTrip(): Observable<Unit> = reportPlannedTrip.execute(
//      selectedTrip = selectedTrip,
//      getVisibleTripGroups = { tripGroups.asObservable() },
//      getSource = tripSource.asObservable(),
//      getChoiceSet = { trip, visibleTripGroups -> getChoiceSet.execute(trip, visibleTripGroups) },
//      userInfoRepository = userInfoRepository)

    fun setInitialSelectedTripGroupId(tripGroupId: String) {
        currentTripGroupId.set(tripGroupId)
    }

    fun onStart() {
//    updateTripProgressSubscription = isLocationPermissionGranted()
//        .filter { granted -> granted }.firstOrError()
//        .flatMap { updateTripProgress.execute(selectedTrip).singleOrError() }
//        .subscribe({}, errorLogger::trackError)
    }

    fun onStop() {
        updateTripProgressSubscription?.dispose()
    }

    fun updateTripGroupResult(tripGroup: List<TripGroup>) {
        tripGroups.accept(tripGroup)
    }
}
