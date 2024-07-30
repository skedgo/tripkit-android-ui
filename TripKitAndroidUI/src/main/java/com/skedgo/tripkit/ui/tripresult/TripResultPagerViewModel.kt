package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.analytics.TripSource
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository
import com.skedgo.tripkit.ui.favorites.waypoints.WaypointRepository
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

const val ARG_TRIP_GROUP_ID = "tripGroupId"

class TripResultPagerViewModel @Inject internal constructor(
    private val context: Context,
    private val getSortedTripGroups: GetSortedTripGroups,
//        private val reportPlannedTrip: ReportPlannedTrip,
    private val trackViewingTrip: TrackViewingTrip,
    private val errorLogger: ErrorLogger,
//        private val eventTracker: EventTracker,
    private val selectedTripGroupRepository: SelectedTripGroupRepository,
//        private val userInfoRepository: UserInfoRepository,
    private val updateTripProgress: UpdateTripProgressWithUserLocation,
    private val tripGroupRepository: TripGroupRepository,
    private val fetchingRealtimeStatusRepository: FetchingRealtimeStatusRepository,
    private val schedulers: SchedulerFactory,
    private val waypointsRepository: WaypointRepository,
    private val favoritesRepository: FavoritesRepository
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

    var currentTrip: MutableLiveData<Trip?> = MutableLiveData(null)

    val isLoading = ObservableField<Boolean>()

    var defaultTrip: Trip? = null

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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getSortedTripGroups(
        args: PagerFragmentArguments,
        initialList: List<TripGroup>
    ): Observable<Unit> {
        isLoading.set(true)
        when (args) {
            is FromRoutes -> {
                return if (!initialList.isNullOrEmpty()) {
                    tripGroups.accept(initialList)
                    isLoading.set(false)
                    tripGroups.map {
                        Unit
                    }
                } else {
                    getSortedTripGroups.execute(
                        args.requestId,
                        args.arriveBy,
                        args.sortOrder,
                        tripResultTransportViewFilter
                    )
                        .subscribeOn(Schedulers.io())
                        .doOnNext {
                            tripGroups.accept(it)
                            isLoading.set(false)
                        }
                        .map { Unit }
                }

            }
            is SingleTrip -> {
                selectedTripGroupRepository.setSelectedTripGroupId(args.tripGroupId)
                return selectedTripGroupRepository.getSelectedTripGroup().map { listOf(it) }
                    .doOnNext { groups ->
                        args.tripId?.let { tripId ->
                            groups.firstOrNull { it.trips?.any { it.id == tripId } == true }
                                ?.let { group ->
                                    defaultTrip = group.trips?.firstOrNull { it.id == tripId }
                                }
                        }
                        tripGroups.accept(groups)
                        isLoading.set(false)
                    }
                    .map { Unit }
            }
            is FavoriteTrip -> {
                fetchingRealtimeStatus.set(true)
                val result = runBlocking {
                    favoritesRepository.getFavoriteById(args.favoriteTripId)
                        .map { it.pattern.orEmpty() }
                        .flatMapLatest { waypoints ->
                            waypointsRepository.getTripGroup(waypoints)
                        }.onEach {
                            it?.trips?.firstOrNull { trip -> trip.uuid() == args.favoriteTripId }
                                ?.let { trip ->
                                    it.displayTripId = trip.id
                                }
                            it?.let { group -> setInitialSelectedTripGroupId(group.uuid()) }
                        }.map {
                            it?.let { listOf(it) } ?: emptyList()
                        }.onEach {
                            currentTrip.postValue(
                                it.firstOrNull {
                                    it.trips?.isNotEmpty() == true
                                }?.trips?.firstOrNull()
                            )
                            tripGroups.accept(it)
                            isLoading.set(false)
                        }.map { Unit }
                        .collect {}
                }
                return Observable.just(result)
            }
            else -> {
                throw IllegalArgumentException("Unknown Argument: $args")
            }
        }
    }

    fun observeInitialPage(): Observable<Unit> {
        return Observables.combineLatest(
            tripGroups.firstOrError().toObservable(),
            selectedTripGroup.hide().firstOrError().toObservable()
        )
        { tripGroups: List<TripGroup>, id: TripGroup ->
            currentTrip.postValue(defaultTrip ?: tripGroups.firstOrNull()?.trips?.first())
            tripGroups.indexOfFirst { id.uuid() == it.uuid() }
        }.doOnNext {
            currentPage.set(it)
        }.map { Unit }
    }

    fun observeTripGroups(): Observable<List<TripGroup>> {
        return tripGroups
            .doOnNext {
                Log.i("viewModel", "tripGroupsBinding set")
                tripGroupsBinding.set(it)
            }
    }

    fun updateSelectedTripGroup(): Observable<TripGroup> {
        return currentPage
            .asObservable()
            .skip(1)
            .withLatestFrom(tripGroups.hide(), BiFunction<Int, List<TripGroup>, TripGroup>
            { id, tripGroups -> tripGroups[id] })
            .observeOn(Schedulers.computation())
            .doOnNext {
                setInitialSelectedTripGroupId(it.uuid())
            }
            .map { it }
    }

    fun getCurrentDisplayTrip(): Observable<Trip> {
        return currentPage
            .asObservable()
            .skip(1)
            .withLatestFrom(tripGroups.hide(), BiFunction<Int, List<TripGroup>, TripGroup>
            { id, tripGroups ->
                if (id > 0) {
                    tripGroups[id]
                } else {
                    tripGroups.first()
                }
            })
            .observeOn(Schedulers.computation())
            .doOnNext {
                setInitialSelectedTripGroupId(it.uuid())
            }
            .map { it.displayTrip }
    }

    fun loadFetchingRealtimeStatus(): Observable<Boolean> {
        return selectedTripGroup
            .switchMap { fetchingRealtimeStatusRepository.get(it.uuid()) }
            .doOnNext { fetchingRealtimeStatus.set(it) }
    }

    fun trackViewingTrip() = trackViewingTrip.execute(tripSource.hide())

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
