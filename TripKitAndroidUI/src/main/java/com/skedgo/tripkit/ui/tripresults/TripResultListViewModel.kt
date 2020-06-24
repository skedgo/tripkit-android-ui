package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.loader.content.Loader
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.RoutingError
import com.skedgo.tripkit.TransportModeFilter
import com.skedgo.tripkit.a2brouting.RouteService
import com.skedgo.tripkit.common.model.TransportMode
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
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.dateTimeZone
import com.skedgo.tripkit.routingstatus.RoutingStatus
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.routingstatus.Status
import com.skedgo.tripkit.ui.core.OnResultStateListener
import com.skedgo.tripkit.ui.creditsources.CreditSourcesOfDataViewModel
import com.skedgo.tripkit.ui.routing.SimpleTransportModeFilter
import com.skedgo.tripkit.ui.tripresult.TripSegmentItemViewModel
import com.skedgo.tripkit.ui.views.MultiStateView
import me.tatarka.bindingcollectionadapter2.collections.MergeObservableList
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
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
    val loadingItem = LoaderPlaceholder()
    val fromName = ObservableField<String>()
    val toName = ObservableField<String>()
    val timeLabel = ObservableField<String>()

    val onItemClicked = PublishRelay.create<ViewTrip>()
    val onMoreButtonClicked = PublishRelay.create<Trip>()

    val stateChange = PublishRelay.create<MultiStateView.ViewState>()
    val onError = PublishRelay.create<String>()

//    val itemBinding = ItemBinding.of<TripResultViewModel>(BR.viewModel, R.layout.trip_result_list_item)
    val itemBinding = ItemBinding.of(
        OnItemBindClass<Any>()
                .map(TripResultViewModel::class.java, BR.viewModel, R.layout.trip_result_list_item)
                .map(LoaderPlaceholder::class.java, ItemBinding.VAR_NONE, R.layout.circular_progress_loader))
    val results = DiffObservableList<TripResultViewModel>(GroupDiffCallback)
    val mergedList = MergeObservableList<Any>().insertList(results)

    val transportBinding = ItemBinding.of<TripResultTransportItemViewModel>(BR.viewModel, R.layout.trip_result_list_transport_item)
    val transportModes: ObservableField<List<TripResultTransportItemViewModel>> = ObservableField(emptyList())
    val showTransport = ObservableBoolean(false)
    val showTransportModeSelection = ObservableBoolean(true)
    val isError = ObservableBoolean(false)
    val showCloseButton = ObservableBoolean(false)
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

    private fun setLoading(loading: Boolean) {
        if (loading && !mergedList.contains(loadingItem)) {
            mergedList.insertItem(loadingItem)
        } else if (!loading && mergedList.contains(loadingItem)) {
            mergedList.removeItem(loadingItem)
        }
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
        setLoading(true)
        regionService.getTransportModesByLocationsAsync(query.fromLocation!!, query.toLocation!!)
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
                        // The transportVisibilityFilter will save walking vs wheelchair automatically,
                        // but we need to manually fix the display, as walking and wheelchair are mutually exclusive.
                        if (it.first == TransportMode.ID_WALK) {
                            toggleTransportModeChecked(TransportMode.ID_WHEEL_CHAIR, false)
                        } else if (it.first == TransportMode.ID_WHEEL_CHAIR) {
                            toggleTransportModeChecked(TransportMode.ID_WALK, false)
                        }

                        transportVisibilityFilter!!.setSelected(it.first, it.second)
                        reload()
                    }.autoClear()
            it
        }
        .toList()
        .subscribe ({ list ->
            transportModes.set(list)
            load()
        }, {
            Timber.e(it)
            if (it.message != null) {
                onError.accept(it.message)
            } else {
                onError.accept("Invalid Response")
            }
        })
        .autoClear()
    }

    private fun toggleTransportModeChecked (mode: String, checked: Boolean) {
        transportModes.get()?.forEach { model ->
            if (model.modeId.get() == mode) {
                model.checked.set(checked)
            }
        }
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
        query.setUseWheelchair(transportVisibilityFilter!!.isSelected(TransportMode.ID_WHEEL_CHAIR))
        Observable.defer {
            routeService.routeAsync(query = query, transportModeFilter = TripResultListViewTransportModeFilter(transportModeFilter!!, transportVisibilityFilter!!))
                    .flatMap {
                        tripGroupRepository.addTripGroups(query.uuid(), it)
                                .toObservable<List<TripGroup>>()
                    }
        }.observeOn(AndroidSchedulers.mainThread())
         .doOnSubscribe {
            setLoading(true)
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
            setLoading(false)
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
                                }.autoClear()
                        vm.onMoreButtonClicked.observable
                                .subscribe {viewModel ->
                                    onMoreButtonClicked.accept(viewModel.trip)
                                }.autoClear()
                        vm

                    }
                }
                .map {
                    Pair(it, results.calculateDiff(it))
                }
                .subscribe {
                    results.update(it.first, it.second)
                    if (results.isEmpty() && !mergedList.contains(loadingItem) && !isError.get()) {
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
