package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.content.res.Resources
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.isExecuting
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.model.TimetableHeaderLineItem
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographer
import com.skedgo.tripkit.ui.utils.Optional
import com.skedgo.tripkit.ui.utils.ignoreNetworkErrors
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import me.tatarka.bindingcollectionadapter2.ItemBinding
import org.joda.time.DateTimeZone
import com.skedgo.tripkit.routing.toSeconds
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.favorites.GetTripFromWaypoints
import com.skedgo.tripkit.ui.favorites.trips.Waypoint
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.views.MultiStateView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirstOrNull
import kotlinx.coroutines.withContext
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.max

data class ShowTimetableEntry(
    val tripGroup: TripGroup,
    val trip: Trip,
    val tripSegment: TripSegment
)

class TimetableViewModel @Inject constructor(
        private val realTimeChoreographer: RealTimeChoreographer,
        private val fetchAndLoadTimetable: FetchAndLoadTimetable,
        private val serviceViewModelProvider: Provider<ServiceViewModel>,
        private val regionService: RegionService,
        private val createShareContent: CreateShareContent,
        private val getNow: GetNow,
        private val resources: Resources,
        private val getRoutingConfig: GetRoutingConfig,
        private val getTripFromWaypoints: GetTripFromWaypoints,
        private val tripGroupRepository: TripGroupRepository
) : RxViewModel() {
    var stop: BehaviorRelay<ScheduledStop> = BehaviorRelay.create()
    var serviceTripId: BehaviorRelay<String> = BehaviorRelay.create()

    val stationName = ObservableField<String>()
    val stationType = ObservableField<String>()
    val itemBinding =
            ItemBinding.of<ServiceViewModel>(BR.viewModel, R.layout.timetable_fragment_list_item)
    val serviceItemBinding =
            ItemBinding.of<TimetableHeaderLineItem>(BR.data, R.layout.timetable_header_line_item)

    private val _showTimetableEntry = MutableLiveData<ShowTimetableEntry>()
    val showTimeTableEntry: LiveData<ShowTimetableEntry> = _showTimetableEntry

    private val _showUpdateLoader = MutableLiveData<Boolean>()
    val showUpdateLoader: LiveData<Boolean> = _showUpdateLoader

    object ServicesDiffCallback : DiffUtil.ItemCallback<ServiceViewModel>() {
        override fun areItemsTheSame(
                oldItem: ServiceViewModel,
                newItem: ServiceViewModel
        ): Boolean =
                oldItem.service.serviceTripId == newItem.service.serviceTripId
                        && oldItem.service.startTimeInSecs == newItem.service.startTimeInSecs


        override fun areContentsTheSame(
                oldItem: ServiceViewModel,
                newItem: ServiceViewModel
        ): Boolean =
                oldItem.serviceNumber.get() == newItem.serviceNumber.get()
                        && oldItem.secondaryText.get() == newItem.secondaryText.get()
                        && oldItem.tertiaryText.get() == newItem.tertiaryText.get()
                        && oldItem.countDownTimeText.get() == newItem.countDownTimeText.get()


    }

    val services = DiffObservableList<ServiceViewModel>(ServicesDiffCallback)

    val serviceNumbers: ObservableField<List<TimetableHeaderLineItem>> =
            ObservableField(emptyList())
    val showLoading = ObservableBoolean(false)
    val showCloseButton = ObservableBoolean(false)
    val showSearch = ObservableBoolean(true)

    val downloadTimetable: PublishRelay<Long> = PublishRelay.create<Long>()
    val onDateChanged: PublishRelay<Long> = PublishRelay.create<Long>()

    val stateChange = PublishRelay.create<MultiStateView.ViewState>()
    val onError = PublishRelay.create<String>()

    val filter: BehaviorRelay<String> = BehaviorRelay.createDefault<String>("")

    private val loadMore = PublishRelay.create<Unit>()

    private val regionObservable = stop.flatMap {
        regionService.getRegionByLocationAsync(it)
    }

    private val currentServiceTripId = serviceTripId.flatMap {
        Observable.just(it)
    }
    private var _currentServiceTripId: String? = null

    val minStartTime = onDateChanged.mergeWith(downloadTimetable).map { it }

    private val servicesAndParentStop = Observables
            .combineLatest(minStartTime, currentServiceTripId, regionObservable, stop)
            { sinceTimeInSecs, currentServiceTripId, region, stop ->
                val time = if (currentServiceTripId.isNullOrEmpty()) {
                    sinceTimeInSecs / 1000
                } else {
                    sinceTimeInSecs
                }
                Triple(time, region, stop)
            }
            .switchMap { (sinceTimeInSecs, region, stop) ->
                Flowable.create<Pair<List<TimetableEntry>, Optional<ScheduledStop>>>({ emitter ->
                    val timeInSecs = AtomicLong(sinceTimeInSecs)
                    val subscription = loadMore
                            .startWith(Unit)
                            .switchMap {
                                fetchAndLoadTimetable.execute(
                                        stop.embarkationStopCode,
                                        stop.disembarkationStopCode,
                                        region,
                                        timeInSecs.get()
                                )
                                        .toObservable()
                                        .ignoreNetworkErrors()
                                        .isExecuting { showLoading.set(it) }
                            }
                            .subscribe({
                                emitter.onNext(it)
                                timeInSecs.set(it.first.last().startTimeInSecs + 1)
                            }, {
                                if(BuildConfig.DEBUG){
                                    it.printStackTrace()
                                }
                                emitter.onError(it)
                            })
                    emitter.setCancellable { subscription.dispose() }
                }, BackpressureStrategy.LATEST).toObservable()
                        .doOnError { throwable: Throwable ->
                            Timber.e("An error occurred", throwable)
                        }
                        .scan { a, b -> (a.first + b.first) to b.second }
            }
            .doOnError { throwable: Throwable ->
                Timber.e("An error occurred", throwable)
            }
            .replay(1)
            .refCount()

    private val parentStop = servicesAndParentStop
            .map { it.second }
            .ignoreNetworkErrors()
            .withLatestFrom(
                    stop,
                    BiFunction<Optional<ScheduledStop>, ScheduledStop, ScheduledStop> { parentStop: Optional<ScheduledStop>, stop: ScheduledStop ->
                        if (parentStop.value != null && parentStop.value.code == stop.code) {
                            parentStop.value
                        } else {
                            stop
                        }
                    })

    private val realtimeRelay = PublishRelay.create<Unit>()

    private val servicesVMs = Observables.combineLatest(
            servicesAndParentStop.map { it.first },
            regionObservable,
            currentServiceTripId
    ) { services: List<TimetableEntry>, region: Region, currentTripId: String ->
        Triple(services, region, currentTripId)
    }.flatMap {
        val services = it.first
        val region = it.second
        _currentServiceTripId = it.third
        realTimeChoreographer.getRealTimeResultsFromCleanElements(region, elements = services)
                .takeUntil(realtimeRelay)
                .map { vehicles ->
                    services.forEach { service: TimetableEntry ->
                        val vehicle =
                                vehicles.firstOrNull { service.serviceTripId == it.serviceTripId }
                        vehicle?.let {
                            service.realtimeVehicle = it
                        }
                    }
                    services
                }
                .map { it to region }
                .startWith(services to region)
    }.map {
        val services = it.first
        val region = it.second
        val timeZone = DateTimeZone.forID(region.timezone)

        services.map {
            serviceViewModelProvider.get().apply {
                this.setService(_currentServiceTripId ?: "", it, timeZone)
                this.onItemClick.observable.observeOn(AndroidSchedulers.mainThread())
                        .subscribe { entry ->
//                            if (action.isNotEmpty()) {
//
//                            } else {
                            timetableEntryChosen.accept(entry)
//                            }
                        }
            }
        }
    }.map {
        it.sortedBy { it.getRealTimeDeparture() }
    }.let {
        Observables.combineLatest(it, filter.hide())
        { services, filter ->
            val serviceList = services.filter {
                listOf(
                        it.service.serviceNumber,
                        it.service.serviceName,
                        it.service.serviceDirection
                ).filter { it != null }
                        .any { it.orEmpty().contains(filter, ignoreCase = true) }
            }

            serviceList
        }
    }.replay(1).refCount()


    val onAlertClicks: Observable<ArrayList<RealtimeAlert>> = services
            .asObservable()
            .switchMap {
                it.map { it.onAlertsClick.observable }
                        .let {
                            Observable.merge(it)
                        }
            }

    val stopRelay = BehaviorRelay.create<ScheduledStop>()
    val startTimeRelay = BehaviorRelay.create<Long>()

    val scrollToNow: PublishRelay<Int> = PublishRelay.create<Int>()

    val enableButton = ObservableBoolean(true)
    val showButton = ObservableBoolean(false)
    val buttonText = ObservableField<String>()
    val actionChosen = PublishRelay.create<String>()
    val timetableEntryChosen = PublishRelay.create<TimetableEntry>()
    var action = ""

    init {

        parentStop.subscribe(stopRelay::accept) { onError.accept(it.message) }.autoClear()
        minStartTime.subscribe(startTimeRelay::accept) { onError.accept(it.message) }.autoClear()
        servicesVMs
                .ignoreNetworkErrors()
                .subscribe({
                    if (it.isEmpty()) {
                        stateChange.accept(MultiStateView.ViewState.EMPTY)
                        return@subscribe
                    }

                    viewModelScope.launch {
                        withContext(Dispatchers.Default) {
                            val diff = services.calculateDiff(it)
                            withContext(Dispatchers.Main) {
                                services.update(it, diff)
                            }
                        }
                    }
                    val tmpServiceList: MutableList<TimetableHeaderLineItem> =
                            arrayListOf<TimetableHeaderLineItem>()

                    it.forEach {
                        tmpServiceList.add(
                                TimetableHeaderLineItem(
                                        it.serviceNumber.get(),
                                        it.serviceColor.get()
                                )
                        )
                    }
                    serviceNumbers.set(tmpServiceList.distinctBy { it.serviceNumber }
                            .sortedBy { it.serviceNumber })
                }, {
                    Timber.e(it)
                    onError.accept(
                            it.message
                                    ?: resources.getString(R.string.an_unexpected_network_error_has_occurred_dot_please_retry_dot)
                    )
                })
                .autoClear()

        servicesVMs
                .ignoreNetworkErrors()
                .take(1)
                .subscribe({
                    scrollToNow.accept(getFirstNowPosition(it))
                }, {}).autoClear()
    }

    fun withBookingActions(bookingActions: ArrayList<String>?, segment: TripSegment?) {
        bookingActions?.let { actions ->
            when {
                actions.contains("showTicket") -> {
                    action = "showTicket"
                    buttonText.set("Show Ticket")
                }
                actions.contains("book") -> {
                    action = "book"
                    buttonText.set("Book")
                }
            }
        }

        showButton.set(
                action.isNotEmpty() && (segment?.booking?.quickBookingsUrl.isNullOrEmpty()
                        || !segment?.booking?.confirmation?.purchasedTickets().isNullOrEmpty())
        )
    }

    fun downloadMoreTimetableAsync() {
        loadMore.accept(Unit)
    }

    fun getFirstNowPosition(): Int = getFirstNowPosition(services)

    fun stopRealtime() {
        // Known issue: Stopping the realtime updates by doing this means that pausing and resuming the app while the
        // timetable is shown will then no longer show realtime updates.
        realtimeRelay.accept(Unit)
    }

    private fun getFirstNowPosition(services: List<ServiceViewModel>): Int {
        return services.indexOfFirst {
            it.getRealTimeDeparture() - getNow.execute().toSeconds() >= -1L
        }.let { max(it, 0) }
    }

    override fun onCleared() {
        super.onCleared()
        services.forEach {
            it.onCleared()
        }
    }

    fun setText(context: Context) {
        this.stop.value?.let {
            stationName.set(this.stop.value?.name)
            stationType.set(this.stop.value?.type.toString())
        }
    }

    fun getShareUrl(shareUrl: String, stop: ScheduledStop) =
            createShareContent.execute(shareUrl, stop, services.map { it.service })

    /**
     * Handles [TimetableEntry] item click
     * @param entry the selected [TimetableEntry] item
     * @param tripSegment the [TripSegment] where [TimetableEntry] was selected
     */
    // This is added to move the logic for getting the trip and tripGroup using waypoints
    // instead of calling [TripPreviewPagerListener.onTimetableEntryClicked] and have the logic
    // on the listening Activity/Fragment and passing this ViewModels' scope
    fun onTimetableEntryClicked(entry: TimetableEntry, tripSegment: TripSegment?) {
        val waypoints = mutableListOf<Waypoint>()
        var segmentToShowIndex = 0

        tripSegment?.trip?.segments?.filter {
            !it.isContinuation && !it.isStationary
        }?.mapIndexed { index, segment ->
            if(segment == tripSegment) {
                segmentToShowIndex = index
                waypoints.add(Waypoint.parseFromTimetableEntryAndSegment(segment, entry))
            } else {
                Waypoint.parseFromSegment(segment)?.let { waypoints.add(it) }
            }
        }

        getTripFromWaypoints(waypoints, segmentToShowIndex)
    }

    /**
     * Getting the trip using waypoints
     *
     * @param waypoints list of waypoints from the [Trip.getSegments]
     * @param segmentToShowIndex index of the segment where the [TimetableEntry] was selected
     */
    private fun getTripFromWaypoints(waypoints: List<Waypoint>, segmentToShowIndex: Int = 0) {
        viewModelScope.launch {
            try {
                _showUpdateLoader.postValue(true)
                val config = getRoutingConfig.execute()
                getTripFromWaypoints.execute(config, waypoints).awaitFirstOrNull()
                    ?.let { response ->
                        _showUpdateLoader.postValue(false)
                        processWaypointsResponse(response, segmentToShowIndex)
                    }
            } catch (e: Exception) {
                Timber.e(e)
                _showUpdateLoader.postValue(false)
            }
        }
    }

    /**
     * Getting the [TripGroup] and [Trip] from the [GetTripFromWaypoints.WaypointResponse]
     *
     * @param waypoints [GetTripFromWaypoints.WaypointResponse] from [GetTripFromWaypoints.execute]
     * @param segmentToShowIndex index of the segment where the [TimetableEntry] was selected
     */
    private suspend fun processWaypointsResponse(
        waypoints: GetTripFromWaypoints.WaypointResponse,
        segmentToShowIndex: Int = 0
    ) {
        waypoints.tripGroup?.let { tripGroup ->
            tripGroupRepository.addTripGroups(tripGroup.uuid(), listOf(tripGroup)).await()
            val trip = tripGroup.displayTrip ?: tripGroup.trips?.first()
            if (trip != null) {
                _showTimetableEntry.postValue(
                    ShowTimetableEntry(tripGroup, trip, trip.segments[segmentToShowIndex])
                )
            }
        }
    }
}
