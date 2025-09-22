package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.analytics.TripSource
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.endDateTime
import com.skedgo.tripkit.routing.startDateTime
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
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
    private var savedTripGroupUuid: String? = null

    fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.getString(ARG_TRIP_GROUP_ID)?.let {
            setInitialSelectedTripGroupId(it)
        }
    }

    fun onSavedInstanceState(outState: Bundle) {
        if (currentTripGroupId.get() != null) {
            outState.putString(ARG_TRIP_GROUP_ID, currentTripGroupId.get())
        }
        // Save the current trip group UUID and trip UUID to SharedPreferences for restoration
        val currentTripGroups = tripGroups.value
        if (!currentTripGroups.isNullOrEmpty()) {
            val currentPageIndex = currentPage.get()
            if (currentPageIndex >= 0 && currentPageIndex < currentTripGroups.size) {
                val currentTripGroup = currentTripGroups[currentPageIndex]
                saveTripGroupUuidToSharedPrefs(currentTripGroup.uuid())
                
                // Also save the specific trip UUID for precise restoration
                val currentTrip = currentTripGroup.displayTrip
                if (currentTrip != null) {
                    saveTripUuidToSharedPrefs(currentTrip.uuid)
                }
            }
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
                    // Set the correct displayTripId for the selected trip
                    args.tripId?.let { selectedTripId ->
                        initialList.forEach { group ->
                            group.trips?.find { it.tripId == selectedTripId }?.let { selectedTrip ->
                                group.displayTripId = selectedTripId
                                defaultTrip = selectedTrip
                            }
                        }
                    }
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
                            groups.firstOrNull { it.trips?.any { it.tripId == tripId } == true }
                                ?.let { group ->
                                    defaultTrip = group.trips?.firstOrNull { it.tripId == tripId }
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
                        .map { it?.pattern.orEmpty() }
                        .flatMapLatest { waypoints ->
                            waypointsRepository.getTripGroup(waypoints)
                        }.onEach {
                            it?.trips?.firstOrNull { trip -> trip.uuid == args.favoriteTripId }
                                ?.let { trip ->
                                    it.displayTripId = trip.tripId
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
        return tripGroups.firstOrError().toObservable()
            .map { tripGroups: List<TripGroup> ->
                savedTripGroupUuid = getTripGroupUuidFromSharedPrefs()
                val savedTripUuid = getTripUuidFromSharedPrefs()

                // Use saved UUID if available (for restoration), otherwise use defaultTrip
                val targetUuid = savedTripGroupUuid?.let { uuid ->
                    uuid
                } ?: run {
                    // Set currentTrip to defaultTrip for normal navigation (not restoration)
                    currentTrip.postValue(defaultTrip ?: tripGroups.firstOrNull()?.trips?.first())
                    return@map -1
                }
                
                val pageIndex = tripGroups.indexOfFirst { tripGroup -> tripGroup.uuid() == targetUuid }
                
                // If we have a saved trip UUID, find the specific trip and set it as displayTrip
                if (savedTripUuid != null && pageIndex >= 0) {
                    val targetTripGroup = tripGroups[pageIndex]
                    val targetTrip = targetTripGroup.trips?.find { it.uuid == savedTripUuid }
                    if (targetTrip != null) {
                        targetTripGroup.displayTripId = targetTrip.tripId
                    }
                }
                
                pageIndex
            }
            .doOnNext { pageIndex ->
                if (pageIndex >= 0) {
                    currentPage.set(pageIndex)
                    // Only update current trip for page 0, as onPageSelected doesn't fire for page 0
                    // For other pages, onPageSelected will handle the updateCurrentTripForPage call
                    if (pageIndex == 0) {
                        updateCurrentTripForPage(pageIndex)
                    }
                    // Clear the saved UUIDs after successful restoration
                    clearTripGroupUuidFromSharedPrefs()
                    clearTripUuidFromSharedPrefs()
                    savedTripGroupUuid = null
                }
            }
            .map { }
    }

    fun observeTripGroups(): Observable<List<TripGroup>> {
        return tripGroups
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnNext {
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

    /**
     * Update the current trip when page changes
     * This ensures the OnTripUpdatedListener is called when switching between fragments
     */
    fun updateCurrentTripForPage(pageIndex: Int) {
        val currentTripGroups = tripGroups.value
        if (!currentTripGroups.isNullOrEmpty() && pageIndex >= 0 && pageIndex < currentTripGroups.size) {
            val tripGroup = currentTripGroups[pageIndex]
            val trip = tripGroup.displayTrip
            currentTrip.postValue(trip)
        }
    }

    /**
     * Save the trip group UUID to SharedPreferences for restoration
     * 
     * Note: We use SharedPreferences instead of onSaveInstanceState because existing 
     * currentTripGroupId handling doesn't survive restoration state (background process kill) 
     * very well and fragments occasionally get initialized twice, resulting in data 
     * inconsistencies when using onSaveInstanceState.
     */
    private fun saveTripGroupUuidToSharedPrefs(uuid: String) {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME_TRIP_RESTORATION, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(KEY_TRIP_GROUP_UUID, uuid).apply()
    }

    /**
     * Get the saved trip group UUID from SharedPreferences
     */
    private fun getTripGroupUuidFromSharedPrefs(): String? {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME_TRIP_RESTORATION, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_TRIP_GROUP_UUID, null)
    }

    /**
     * Clear the saved trip group UUID from SharedPreferences
     */
    private fun clearTripGroupUuidFromSharedPrefs() {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME_TRIP_RESTORATION, Context.MODE_PRIVATE)
        sharedPrefs.edit().remove(KEY_TRIP_GROUP_UUID).apply()
    }

    /**
     * Save the trip UUID to SharedPreferences for restoration
     */
    private fun saveTripUuidToSharedPrefs(uuid: String) {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME_TRIP_RESTORATION, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(KEY_TRIP_UUID, uuid).apply()
    }

    /**
     * Get the saved trip UUID from SharedPreferences
     */
    private fun getTripUuidFromSharedPrefs(): String? {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME_TRIP_RESTORATION, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_TRIP_UUID, null)
    }

    /**
     * Clear the saved trip UUID from SharedPreferences
     */
    private fun clearTripUuidFromSharedPrefs() {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME_TRIP_RESTORATION, Context.MODE_PRIVATE)
        sharedPrefs.edit().remove(KEY_TRIP_UUID).apply()
    }
    
    companion object {
        // SharedPreferences constants for trip restoration
        private const val SHARED_PREFS_NAME_TRIP_RESTORATION = "trip_restoration"
        private const val KEY_TRIP_GROUP_UUID = "trip_group_uuid"
        private const val KEY_TRIP_UUID = "trip_uuid"
    }
}
