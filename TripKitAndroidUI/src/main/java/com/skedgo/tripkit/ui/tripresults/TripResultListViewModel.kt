package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.RoutingError
import com.skedgo.tripkit.TransportModeFilter
import com.skedgo.tripkit.a2brouting.RouteService
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.routing.GetSortedTripGroupsWithRoutingStatus
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
import com.skedgo.tripkit.ui.core.OnResultStateListener
import com.skedgo.tripkit.ui.routing.SimpleTransportModeFilter
import com.skedgo.tripkit.ui.views.MultiStateView
import timber.log.Timber
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
        private val errorLogger: ErrorLogger,
        private val routingTimeViewModelMapper: RoutingTimeViewModelMapper): RxViewModel() {

    val fromName = ObservableField<String>()
    val toName = ObservableField<String>()
    val timeLabel = ObservableField<String>()

    val onItemClicked = PublishRelay.create<ViewTrip>()

    val stateChange = PublishRelay.create<MultiStateView.ViewState>()
    val onError = PublishRelay.create<String>()

    val itemBinding = ItemBinding.of<TripResultViewModel>(BR.viewModel, R.layout.trip_result_list_item)
    val results = DiffObservableList<TripResultViewModel>(GroupDiffCallback)

    val transportBinding = ItemBinding.of<TripResultTransportItemViewModel>(BR.viewModel, R.layout.trip_result_list_transport_item)
    val transportModes: ObservableField<List<TripResultTransportItemViewModel>> = ObservableField(emptyList())
    val showTransport = ObservableBoolean(false)
    val showTransportModeSelection = ObservableBoolean(true)
    val isLoading = ObservableBoolean(false)
    val isError = ObservableBoolean(false)

    private val transportModeChangeThrottle = PublishSubject.create<Unit>()

    lateinit var query: Query
    private var transportModeFilter: TransportModeFilter? = null
    private var transportVisibilityFilter: TripResultTransportViewFilter? = null

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

    fun setup(_query: Query,
              showTransportSelectionView: Boolean,
              transportModeFilter: TransportModeFilter?) {
        this.query = _query
        _query.fromLocation?.let {
            fromName.set(it.displayName)
        }
        _query.toLocation?.let {
            toName.set(it.displayName)
        }

        showTransportModeSelection.set(showTransportSelectionView)
        transportVisibilityFilter = if (showTransportSelectionView) {
            PrefsBasedTransportViewFilter(context)
        } else {
            PermissiveTransportViewFilter()
        }

        if (transportModeFilter == null) {
            this.transportModeFilter = SimpleTransportModeFilter()
        } else {
            this.transportModeFilter = transportModeFilter
        }

        setTimeLabel()
        getTransport()
    }


    private fun getTransport() {
        isLoading.set(true)
        regionService.getTransportModesByLocationAsync(query.fromLocation!!)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapIterable { value -> value }
                .filter {
                    transportModeFilter!!.useTransportMode(it.id)
                }
                .map { mode ->
                    tripResultTransportItemViewModelProvider.get().apply {
                        this.setup(mode)
                    }
                }
                .map { viewModel ->
                    viewModel.checked.set(transportVisibilityFilter!!.isSelected(viewModel.modeId.get()!!))
                    viewModel
                }
                .map {
                    it.clicked
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                transportVisibilityFilter!!.setSelected(it.first, it.second)
                                loadFromStore()
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


    private fun setTimeLabel() {
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
            routeService.routeAsync(query = query, transportModeFilter = transportModeFilter!!)
                    .flatMap {
                        tripGroupRepository.addTripGroups(query.uuid(), it)
                                .toObservable<List<TripGroup>>()
                    }
        }.doOnSubscribe {
            isLoading.set(true)
            stateChange.accept(MultiStateView.ViewState.CONTENT)
            routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
                    query.uuid(),
                    Status.InProgress()
            )).subscribe()
            loadFromStore()
        }.doOnError {
            val message = when (it) {
                is RoutingError -> it.message
                else -> context.getString(R.string.error_encountered)
            }
            routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
                            query.uuid(),
                            Status.Error(message)
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
        }.subscribe({}, { error ->
            isError.set(true)
            if (error.message.isNullOrBlank()) {
                onError.accept( context.getString(R.string.unknown_error))
            } else {
                onError.accept(error.message)
            }
            Timber.e(error, "An error in routing occurred")
        }).autoClear()
    }

    fun reload() {
        results.update(emptyList())
        load()
    }

    private fun loadFromStore() {
        getSortedTripGroupsWithRoutingStatusProvider.get().execute(query, 1, transportVisibilityFilter!!)
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
                    if (results.isEmpty() && !isLoading.get() && !isError.get()) {
                        stateChange.accept(MultiStateView.ViewState.EMPTY)
                    }
                }.autoClear()

    }

    fun changeQuery(newQuery: Query) {
        results.update(emptyList())
        setup(newQuery, showTransportModeSelection.get(), transportModeFilter)
    }

    fun updateQueryTime(timeTag: TimeTag) {
        val currentQuery = query
        query = currentQuery.clone(true)
        query.setTimeTag(timeTag)
        setTimeLabel()
        reload()
    }



}
