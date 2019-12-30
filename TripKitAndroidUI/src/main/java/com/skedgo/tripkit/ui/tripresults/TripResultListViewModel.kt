package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.RoutingError
import com.skedgo.tripkit.TransitModeFilter
import com.skedgo.tripkit.a2brouting.RouteService
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.routing.GetSortedTripGroupsWithRoutingStatus
import com.skedgo.tripkit.ui.routing.PerformRouting
import com.skedgo.tripkit.ui.routingresults.IsModeIncludedInTripsRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.trip.options.RoutingTimeViewModelMapper
import com.skedgo.tripkit.ui.trip.toRoutingTime
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.dateTimeZone
import com.skedgo.tripkit.routingstatus.RoutingStatus
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.routingstatus.Status
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

class TripResultListViewModel @Inject constructor(
        private val context: Context,
        private val tripGroupRepository: TripGroupRepository,
        private val routingStatusRepositoryLazy: Lazy<RoutingStatusRepository>,
        private val tripResultViewModelProvider: Provider<TripResultViewModel>,
        private val getSortedTripGroupsWithRoutingStatusProvider: Provider<GetSortedTripGroupsWithRoutingStatus>,
        private val tripResultTransportItemViewModelProvider: Provider<TripResultTransportItemViewModel>,
        private val regionService: RegionService,
        private val routeService: RouteService,
        private val transitModeFilter: TransitModeFilter,
        private val errorLogger: ErrorLogger,
        private val getTransportModePreferencesByRegion: GetTransportModePreferencesByRegion,
        private val sorterProvider: Provider<TripGroupsSorter>,
        private val isModeIncludedInTripsRepository: IsModeIncludedInTripsRepository,
        private val performRouting: PerformRouting,
        private val routingTimeViewModelMapper: RoutingTimeViewModelMapper): RxViewModel() {
    val fromName = ObservableField<String>()
    val toName = ObservableField<String>()
    val timeLabel = ObservableField<String>()

    val onItemClicked = PublishRelay.create<ViewTrip>()

    val itemBinding = ItemBinding.of<TripResultViewModel>(BR.viewModel, R.layout.trip_result_list_item)
    val results = DiffObservableList<TripResultViewModel>(GroupDiffCallback)
//val results = ObservableArrayList<TripResultViewModel>()

    val transportBinding = ItemBinding.of<TripResultTransportItemViewModel>(BR.viewModel, R.layout.trip_result_list_transport_item)
    val transportModes: ObservableField<List<TripResultTransportItemViewModel>> = ObservableField(emptyList())
    val showTransport = ObservableBoolean(false)
    val isLoading = ObservableBoolean(false)

    private val transportModeChangeThrottle = PublishSubject.create<Unit>()

    lateinit var query: Query
    init {
        transportModeChangeThrottle.debounce(500, TimeUnit.MILLISECONDS)
                .subscribe(
                        { load() },
                        { errorLogger.trackError(it) })
                .autoClear()

    }
    fun transportLayoutClicked(view: View) {
        showTransport.set(!showTransport.get())
    }

    fun setup(_query: Query) {
        this.query = _query
        _query.fromLocation?.let {
            fromName.set(it.displayName)
        }
        _query.toLocation?.let {
            toName.set(it.displayName)
        }
        setTimeLabel()
        getTransport()
    }


    fun getTransport() {
        isLoading.set(true)
        regionService.getTransportModesByLocationAsync(query.fromLocation!!)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapIterable { item -> item }
                .map { mode ->
                    tripResultTransportItemViewModelProvider.get().apply {
                        this.setup(mode)
                    }
                }
                .map { viewModel ->
                    isModeIncludedInTripsRepository.isModeIncludedForRouting(viewModel.modeId.get()!!)
                            .subscribe { isIncluded -> viewModel.checked.set(isIncluded) }
                    viewModel
                }
                .map {
                    it.clicked
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                isModeIncludedInTripsRepository.setModeIncluded(it.first, it.second)
                                        .doOnComplete {
                                            transportModeChangeThrottle.onNext(Unit)
                                        }.subscribe().autoClear()
                            }.autoClear()
                    it
                }
                .toList()
                .subscribe { list ->
                    transportModes.set(list)
                    load()
                }
                .autoClear()
    }


    fun setTimeLabel() {
        query.timeTag?.let {timeTag ->
            query.fromLocation?.let {
                routingTimeViewModelMapper.toText(timeTag.toRoutingTime(it.dateTimeZone)).toObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {str ->
                            timeLabel.set(str)
                        }.autoClear()
            }
        }
    }

    fun load() {
        query = query.clone(false)

        Observable.defer {
            routeService.routeAsync(query = query, transitModeFilter = transitModeFilter)
                    .flatMap {
                        tripGroupRepository.addTripGroups(query.uuid(), it)
                                .toObservable<List<TripGroup>>()
                    }
        }.doOnSubscribe {
            isLoading.set(true)
            routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
                    query.uuid(),
                    Status.InProgress()
            )).subscribe()
            loadFromStore()
        }.doOnError {
            routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
                            query.uuid(),
                            Status.Error(
                                    when (it) {
                                        is RoutingError -> it.message
                                        else -> context.getString(R.string.error_encountered)
                                    }
                            )
                    )).subscribe()
        }
        .doOnComplete {
            routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
                            query.uuid(),
                            Status.Completed()
                    )).subscribe()
        }
        .doFinally {
            isLoading.set(false)
        }
        .subscribe().autoClear()
    }

    fun reload() {
        results.update(emptyList())
        load()
    }

    fun loadFromStore() {
        getSortedTripGroupsWithRoutingStatusProvider.get().execute(query, 1)
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    val list = it.first
                    val classifier = TripGroupClassifier(list)
                    list.map { group ->
                        val vm = tripResultViewModelProvider.get().apply {
                            this.setTripGroup(context, group, classifier.classify(group))
                        }
                        vm.onItemClicked.observable
                                .subscribe { viewModel ->
                                    val clickEvent = ViewTrip(query = this.query,
                                            tripGroupUUID = viewModel.group.uuid(),
                                            sortOrder = 1, /* TODO Proper sorting */
                                            displayTripID = viewModel.group.displayTripId)
                                    onItemClicked.accept(clickEvent)
                                }
                        vm
                    }
                }
                .map {
                    Pair(it, results.calculateDiff(it))
                }
                .subscribe {
                    results.update(it.first, it.second)
                }.autoClear()

    }

    fun changeQuery(newQuery: Query) {
        results.update(emptyList())
        setup(newQuery)
    }

    fun updateQueryTime(timeTag: TimeTag) {
        val currentQuery = query
        query = currentQuery.clone(true)
        query.setTimeTag(timeTag)
        setTimeLabel()
        reload()
    }



}
