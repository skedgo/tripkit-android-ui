package com.skedgo.tripkit.ui.timetables

import android.content.res.Resources
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
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
import com.skedgo.tripkit.ui.views.MultiStateView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.max

class TimetableViewModel  @Inject constructor(
        private val realTimeChoreographer: RealTimeChoreographer,
        private val fetchAndLoadTimetable: FetchAndLoadTimetable,
        private val serviceViewModelProvider: Provider<ServiceViewModel>,
        private val regionService: RegionService,
        private val createShareContent: CreateShareContent,
        private val getNow: GetNow,
        private val resources: Resources
): RxViewModel() {
    var stop: BehaviorRelay<ScheduledStop> = BehaviorRelay.create<ScheduledStop>()
    var serviceClick = PublishRelay.create<TimetableEntry>()

    val stationName = ObservableField<String>()
    val stationType = ObservableField<String>()
    val itemBinding = ItemBinding.of<ServiceViewModel>(BR.viewModel, R.layout.timetable_fragment_list_item)
    val serviceItemBinding = ItemBinding.of<TimetableHeaderLineItem>(BR.data, R.layout.timetable_header_line_item)

    object ServicesDiffCallback : DiffUtil.ItemCallback<ServiceViewModel>() {
        override fun areItemsTheSame(oldItem: ServiceViewModel, newItem: ServiceViewModel): Boolean =
                    oldItem.service.serviceTripId == newItem.service.serviceTripId
                    && oldItem.service.startTimeInSecs == newItem.service.startTimeInSecs


        override fun areContentsTheSame(oldItem: ServiceViewModel, newItem: ServiceViewModel): Boolean =
            oldItem.serviceNumber.get() == newItem.serviceNumber.get()
                    && oldItem.secondaryText.get() == newItem.secondaryText.get()
                    && oldItem.tertiaryText.get() == newItem.tertiaryText.get()
                    && oldItem.countDownTimeText.get() == newItem.countDownTimeText.get()


    }
    val services = DiffObservableList<ServiceViewModel>(ServicesDiffCallback)

    val serviceNumbers: ObservableField<List<TimetableHeaderLineItem>> = ObservableField(emptyList())
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

    val minStartTime = onDateChanged.mergeWith(downloadTimetable).map { it / 1000 }

    private val servicesAndParentStop = Observables
            .combineLatest(minStartTime, regionObservable, stop)
            { sinceTimeInSecs, region, stop -> Triple(sinceTimeInSecs, region, stop) }
            .switchMap { (sinceTimeInSecs, region, stop) ->
                Flowable.create<Pair<List<TimetableEntry>, Optional<ScheduledStop>>>({ emitter ->
                            val timeInSecs = AtomicLong(sinceTimeInSecs)
                            val subscription = loadMore
                                    .startWith(Unit)
                                    .switchMap {
                                        fetchAndLoadTimetable.execute(stop.embarkationStopCode, region, timeInSecs.get())
                                                .toObservable()
                                                .ignoreNetworkErrors()
                                                .isExecuting { showLoading.set(it) }
                                    }
                                    .subscribe({
                                        emitter.onNext(it)
                                        timeInSecs.set(it.first.last().startTimeInSecs + 1)
                                    }, { emitter.onError(it) })
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
            .withLatestFrom(stop, BiFunction<Optional<ScheduledStop>, ScheduledStop, ScheduledStop> { parentStop : Optional<ScheduledStop> , stop : ScheduledStop ->
                if (parentStop.value != null && parentStop.value.code == stop.code) {
                    parentStop.value
                } else {
                    stop
                }
            })

    private val realtimeRelay = PublishRelay.create<Unit>()

    private val servicesVMs = Observables.combineLatest(servicesAndParentStop.map { it.first }, regionObservable)
                { services : List<TimetableEntry>, region : Region -> services to region }
            .flatMap {
                val services = it.first
                val region = it.second
                realTimeChoreographer.getRealTimeResultsFromCleanElements(region, elements = services)
                     .takeUntil(realtimeRelay)
                     .map { vehicles ->
                            services.forEach { service : TimetableEntry ->
                                val vehicle = vehicles.firstOrNull { service.serviceTripId == it.serviceTripId  }
                                vehicle?.let {
                                    service.realtimeVehicle = it
                                }
                            }
                            services
                        }
                        .map { it to region }
                        .startWith(services to region)
            }
            .map {
                val services = it.first
                val region = it.second
                val timeZone = DateTimeZone.forID(region.timezone)

                services.map {
                    serviceViewModelProvider.get().apply {
                        this.setService(it, timeZone)
                    }
                }
            }
            .map { it.sortedBy { it.getRealTimeDeparture() } }
            .let {
                Observables.combineLatest(it, filter.hide())
                { services, filter ->
                    val serviceList = services.filter {
                        listOf(it.service.serviceNumber, it.service.serviceName, it.service.serviceDirection).filter { it != null }
                                .any { it.orEmpty().contains(filter, ignoreCase = true) }
                    }

                    serviceList
                }
            }
            .replay(1)
            .refCount()


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
    val onServiceClick = services.asObservable()
            .switchMap { vMs ->
                vMs.map { it.onItemClick.observable }
                .let { Observable.merge(it) }
//                .map { Triple(it, stopRelay.value, minStartTime) }
            }


    val scrollToNow: PublishRelay<Int> = PublishRelay.create<Int>()

    val enableButton = ObservableBoolean(true)
    val showButton = ObservableBoolean(false)
    val buttonText = ObservableField<String>()
    val actionChosen = PublishRelay.create<String>()
    var action = ""

    init {

        parentStop.subscribe(stopRelay::accept) { onError.accept(it.message)}
        minStartTime.subscribe(startTimeRelay::accept) { onError.accept(it.message)}
        servicesVMs
                .ignoreNetworkErrors()
            .subscribe ({
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
                    val tmpServiceList : MutableList<TimetableHeaderLineItem> = arrayListOf<TimetableHeaderLineItem>()

                    it.forEach {
                        tmpServiceList.add(TimetableHeaderLineItem(it.serviceNumber.get(), it.serviceColor.get()))
                    }
                    serviceNumbers.set(tmpServiceList.distinctBy { it.serviceNumber }.sortedBy { it.serviceNumber })
                }, {
                    Timber.e(it)
                    onError.accept(it.message ?: resources.getString(R.string.an_unexpected_network_error_has_occurred_dot_please_retry_dot))
                })
                .autoClear()

        servicesVMs
                .ignoreNetworkErrors()
                .take(1)
                .subscribe ({
                    scrollToNow.accept(getFirstNowPosition(it))
                }, {}).autoClear()
    }

    fun withSegment(bookingActions: ArrayList<String>?) {
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

        showButton.set(action.isNotEmpty())
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

    fun setText() {
        this.stop.value?.let {
            stationName.set(this.stop.value?.name)
            stationType.set(this.stop.value?.type.toString())
        }
    }

    fun getShareUrl(stop: ScheduledStop) =
            createShareContent.execute(stop, services.map { it.service })

}
