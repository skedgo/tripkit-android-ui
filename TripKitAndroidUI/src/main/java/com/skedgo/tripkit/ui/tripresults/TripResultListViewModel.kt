package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.view.View
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
import com.skedgo.tripkit.routingstatus.RoutingStatus
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.routingstatus.Status
import com.skedgo.tripkit.ui.model.UserMode
import com.skedgo.tripkit.ui.routing.SimpleTransportModeFilter
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonContainer
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.skedgo.tripkit.ui.views.MultiStateView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.bindingcollectionadapter2.collections.MergeObservableList
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import org.joda.time.DateTimeZone
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

class TripResultListViewModel @Inject constructor(
    val context: Context,
    private val tripGroupRepository: TripGroupRepository,
    private val routingStatusRepositoryLazy: Lazy<RoutingStatusRepository>,
    private val tripResultViewModelProvider: Provider<TripResultViewModel>,
    private val getSortedTripGroupsWithRoutingStatusProvider: Provider<GetSortedTripGroupsWithRoutingStatus>,
    private val tripResultTransportItemViewModelProvider: Provider<TripResultTransportItemViewModel>,
    private val regionService: RegionService,
    private val routeService: RouteService,
    private val errorLogger: ErrorLogger,
    private val routingTimeViewModelMapper: RoutingTimeViewModelMapper
) : RxViewModel(), ActionButtonContainer {
    val loadingItem = LoaderPlaceholder()
    val fromName = ObservableField<String>()
    val fromContentDescription = ObservableField<String>()
    val toName = ObservableField<String>()
    val toContentDescription = ObservableField<String>()
    val timeLabel = ObservableField<String>()

    val onItemClicked = PublishRelay.create<ViewTrip>()
    val onMoreButtonClicked = PublishRelay.create<Trip>()
    val onFinished = PublishRelay.create<Boolean>()

    val stateChange = PublishRelay.create<MultiStateView.ViewState>()
    val onError = PublishRelay.create<String>()

    //    val itemBinding = ItemBinding.of<TripResultViewModel>(BR.viewModel, R.layout.trip_result_list_item)
    val itemBinding =
        ItemBinding.of(
            OnItemBindClass<Any>()
                .map(TripResultViewModel::class.java, BR.viewModel, R.layout.trip_result_list_item)
                .map(
                    LoaderPlaceholder::class.java,
                    ItemBinding.VAR_NONE,
                    R.layout.circular_progress_loader
                )
        )

    val results = DiffObservableList<TripResultViewModel>(GroupDiffCallback)
    private val loadingList = ObservableArrayList<LoaderPlaceholder>()
    val mergedList = MergeObservableList<Any>().insertList(loadingList).insertList(results)

    val transportBinding = ItemBinding.of<TripResultTransportItemViewModel>(
        BR.viewModel,
        R.layout.trip_result_list_transport_item
    )
    val transportModes: ObservableField<List<TripResultTransportItemViewModel>> =
        ObservableField(emptyList())
    val showTransport = ObservableBoolean(false)
    val showTransportModeSelection = ObservableBoolean(true)
    val isError = ObservableBoolean(false)
    val showCloseButton = ObservableBoolean(false)
    private val transportModeChangeThrottle = PublishSubject.create<Unit>()

    val tripGroupList = ObservableArrayList<TripGroup>()
    var tripGroupWithUrlList = arrayListOf<TripGroup>()

    lateinit var query: Query
    private var transportModeFilter: TransportModeFilter? = null
    private var transportVisibilityFilter: TripResultTransportViewFilter? = null
    private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null
    private val networkRequests = CompositeDisposable()
    private var replaceModes: List<UserMode>? = null

    private val _helpInfoVisible = MutableLiveData<Boolean>(true)
    val helpInfoVisible: LiveData<Boolean> = _helpInfoVisible
    private val _showHelpInfo = MutableLiveData<Boolean>()
    val showHelpInfo: LiveData<Boolean> = _showHelpInfo

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
            loadingList.add(loadingItem)
        } else if (!loading && mergedList.contains(loadingItem)) {
            loadingList.clear()
        }
    }

    fun setReplaceMode(list: List<UserMode>) {
        replaceModes = list
    }

    fun setup(
        _query: Query,
        showTransportSelectionView: Boolean,
        transportModeFilter: TransportModeFilter?,
        actionButtonHandlerFactory: ActionButtonHandlerFactory?,
        force: Boolean = false,
        execute: Boolean = true
    ) {
        if (!force && mergedList.size > 0) {
            return
        }
        this.query = _query
        _query.fromLocation?.let {
            val displayName = it.displayName
            fromName.set(displayName)
            fromContentDescription.set("From $displayName")
        }
        _query.toLocation?.let {
            val displayName = it.displayName
            toName.set(displayName)
            toContentDescription.set("Going to $displayName")
        }

        showTransportModeSelection.set(showTransportSelectionView)
        transportVisibilityFilter = if (showTransportSelectionView) {
            PrefsBasedTransportViewFilter(context)
        } else {
            PermissiveTransportViewFilter()
        }
        this.actionButtonHandlerFactory = actionButtonHandlerFactory
        if (transportModeFilter == null) {
            val filter = SimpleTransportModeFilter()
            replaceModes?.let {
                filter.replaceTransportModesWithUserModes(it)
            }

            this.transportModeFilter = filter
        } else {
            this.transportModeFilter = transportModeFilter
        }

        setTimeLabel()
        getTransport(execute)
    }


    private fun getTransport(execute: Boolean = true) {
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
                    .subscribe { type ->
                        // The transportVisibilityFilter will save walking vs wheelchair automatically,
                        // but we need to manually fix the display, as walking and wheelchair are mutually exclusive.
                        if (type.first == TransportMode.ID_WALK) {
                            toggleTransportModeChecked(TransportMode.ID_WHEEL_CHAIR, false)
                        } else if (type.first == TransportMode.ID_WHEEL_CHAIR) {
                            toggleTransportModeChecked(TransportMode.ID_WALK, false)
                            if (!type.second) {
                                toggleTransportModeChecked(TransportMode.ID_BICYCLE, true)
                                toggleTransportModeChecked(TransportMode.ID_MOTORBIKE, true)
                            }
                        }

                        transportVisibilityFilter!!.setSelected(type.first, type.second)
                        reload()
                    }.autoClear()
                it
            }
            .toList()
            .subscribe({ list ->
                transportModes.set(list)
                if (execute) {
                    load()
                }
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

    private fun toggleTransportModeChecked(mode: String, checked: Boolean) {
        transportModes.get()?.forEach { model ->
            if (model.modeId.get() == mode) {
                model.checked.set(checked)
            }
        }
    }

    private fun setTimeLabel() {
        query.timeTag?.let { timeTag ->
            try {
                query.fromLocation?.let { fromLocation ->
                    if (fromLocation.timeZone == null) {
                        regionService.getRegionByLocationAsync(fromLocation)
                            .map { it.timezone }
                    } else {
                        Observable.just(fromLocation.timeZone)
                    }.flatMap { timeZone ->
                        val dateTimeZone = DateTimeZone.forID(timeZone)
                        routingTimeViewModelMapper.toText(timeTag.toRoutingTime(dateTimeZone))
                            .toObservable()
                    }.observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ str ->
                            timeLabel.set(str)
                        }, { error ->
                            isError.set(true)
                            if (error.message.isNullOrBlank()) {
                                onError.accept(context.getString(R.string.unknown_error))
                            } else {
                                onError.accept(error.message)
                            }
                            Timber.e(error, "An error in routing occurred ${error.message}")
                        }).autoClear()


                }
            } catch (_: Exception) {
            }
        }
    }

    fun load() {
        query = query.clone(true)
        query.setUseWheelchair(transportVisibilityFilter!!.isSelected(TransportMode.ID_WHEEL_CHAIR))
        val request = Observable.defer {
            val filter = TripResultListViewTransportModeFilter(
                transportModeFilter!!,
                transportVisibilityFilter!!
            )
            replaceModes?.let {
                filter.replaceTransportModes(it)
            }

            routeService.routeAsync(query = query, transportModeFilter = filter)
                .flatMap {
                    tripGroupWithUrlList.addAll(it)
                    tripGroupRepository.addTripGroups(query.uuid(), it)
                        .toObservable<List<TripGroup>>()
                }
        }.observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                setLoading(true)
                stateChange.accept(MultiStateView.ViewState.CONTENT)
                routingStatusRepositoryLazy.get().putRoutingStatus(
                    RoutingStatus(
                        query.uuid(),
                        Status.InProgress()
                    )
                ).subscribe()
                loadFromStore()
            }.doOnError {
                val message = when (it) {
                    is RoutingError -> it.message
                    else -> context.getString(R.string.error_encountered)
                }
                routingStatusRepositoryLazy.get().putRoutingStatus(
                    RoutingStatus(
                        query.uuid(),
                        Status.Error(message)
                    )
                ).subscribe()
            }
            .doOnComplete {
                routingStatusRepositoryLazy.get().putRoutingStatus(
                    RoutingStatus(
                        query.uuid(),
                        Status.Completed()
                    )
                ).subscribe()
            }
            .doFinally {
                onFinished.accept(true)
                setLoading(false)
            }.subscribe({}, { error ->
                isError.set(true)
                if (error.message.isNullOrBlank()) {
                    onError.accept(context.getString(R.string.unknown_error))
                } else {
                    onError.accept(error.message)
                }
                Timber.e(error, "An error in routing occurred ${error.message}")
            })

        networkRequests.add(request)
        request.autoClear()
    }

    fun reload() {
        networkRequests.clear()
        results.update(emptyList())
        load()
    }

    private fun loadFromStore() {
        val tripFlow = MutableSharedFlow<Trip>()
        tripFlow.onEach {
            val clickEvent = ViewTrip(
                query = this.query,
                tripGroupUUID = it.group.uuid(),
                sortOrder = 1, /* TODO Proper sorting */
                displayTripID = it.id
            )
            onItemClicked.accept(clickEvent)
        }.launchIn(viewModelScope)

        getSortedTripGroupsWithRoutingStatusProvider.get()
            .execute(query, 1, transportVisibilityFilter!!)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                var list = it.first

                tripGroupList.clear()

                // Compare with tempTripGroupList and add fullUrl if it matches
                list.forEach { group ->
                    val matchingGroup =
                        tripGroupWithUrlList.find { tempGroup -> tempGroup.uuid() == group.uuid() }
                    if (matchingGroup != null) {
                        group.fullUrl = matchingGroup.fullUrl
                    }
                }

                tripGroupList.addAll(list)
                val classifier = TripGroupClassifier(list)
                list.map { group ->
                    val vm = tripResultViewModelProvider.get().apply {
                        var handler: ActionButtonHandler? =
                            actionButtonHandlerFactory?.createHandler(this@TripResultListViewModel)
                        this.actionButtonHandler = handler
                        this.clickFlow = tripFlow
                        this.setTripGroup(context, group, classifier.classify(group))
                        onMoreButtonClicked.observable
                            .subscribe {
                                if (it.otherTripGroups.isNullOrEmpty()) {
                                    actionButtonHandler?.primaryActionClicked(it.trip)
                                } else {
                                    it.toggleShowMore()
                                }
                            }.autoClear()
                    }

                    vm
                }.sortedByDescending { it.classification.ordinal }
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
        networkRequests.clear()
        setup(
            newQuery,
            showTransportModeSelection.get(),
            transportModeFilter,
            actionButtonHandlerFactory,
            true
        )
    }

    fun updateQueryTime(timeTag: TimeTag) {
        val currentQuery = query
        query = currentQuery.clone(true)
        query.setTimeTag(timeTag)
        setTimeLabel()
        reload()
    }

    override fun scope() = viewModelScope

    override fun replaceTripGroup(tripGroupUuid: String, newTripGroup: TripGroup) {
        results.forEach {
            if (it.group.uuid() == tripGroupUuid) {
                it.setTripGroup(context, newTripGroup, null)
                return@forEach
            }
        }
    }

    fun setHelpInfoVisibility(show: Boolean) {
        _helpInfoVisible.postValue(show)
    }

    fun onShowBookARideInduction(show: Boolean) {
        _showHelpInfo.postValue(show)
    }

}
