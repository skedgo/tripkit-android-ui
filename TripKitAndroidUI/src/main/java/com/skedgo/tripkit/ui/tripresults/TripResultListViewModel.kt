package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.view.View
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.LOCATION_NOT_SUPPORTED_ERROR
import com.skedgo.tripkit.RoutingError
import com.skedgo.tripkit.TransportModeFilter
import com.skedgo.tripkit.a2brouting.RouteService
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.model.time.TimeTag
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routingstatus.RoutingStatus
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.routingstatus.Status
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.model.UserMode
import com.skedgo.tripkit.ui.routing.GetSortedTripGroupsWithRoutingStatus
import com.skedgo.tripkit.ui.routing.SimpleTransportModeFilter
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.trip.options.RoutingTimeViewModelMapper
import com.skedgo.tripkit.ui.trip.toRoutingTime
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonContainer
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.skedgo.tripkit.ui.views.MultiStateView
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
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
    val fromName = MutableLiveData<String>()
    val fromContentDescription = MutableLiveData<String>()
    val toName = MutableLiveData<String>()
    val toContentDescription = MutableLiveData<String>()
    val timeLabel = MutableLiveData<String>()

    val onItemClicked = PublishRelay.create<ViewTrip>()
    val onQuickBookingActionClicked = PublishRelay.create<TripSegment>()
    val onMoreButtonClicked = PublishRelay.create<Trip>()
    val onFinished = PublishRelay.create<Boolean>()

    val stateChange = PublishRelay.create<MultiStateView.ViewState>()
    val onError = PublishRelay.create<String>()

    val customAdapter = TripResultListCustomRecyclerViewAdapter<Any>()

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
    val tripResultListStream = BehaviorSubject.create<List<TripResultViewModel>>()

    private val loadingList = ObservableArrayList<LoaderPlaceholder>()
    val mergedList = MergeObservableList<Any>().insertList(loadingList).insertList(results)

    val transportBinding = ItemBinding.of<TripResultTransportItemViewModel>(
        BR.viewModel,
        R.layout.trip_result_list_transport_item
    )
    val transportModes: MutableLiveData<List<TripResultTransportItemViewModel>> =
        MutableLiveData(emptyList())
    val showTransport = ObservableBoolean(false)
    val showTransportModeSelection = ObservableBoolean(true)
    val isError = ObservableBoolean(false)
    val showCloseButton = ObservableBoolean(false)
    private val transportModeChangeThrottle = PublishSubject.create<Unit>()
    private val resultListUpdateThrottle = PublishSubject.create<Unit>()

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

    private val _startLocationListener = MutableLiveData<Boolean>()
    val startLocationListener: LiveData<Boolean> get() = _startLocationListener

    init {
        transportModeChangeThrottle.debounce(500, TimeUnit.MILLISECONDS)
            .subscribe(
                { load() },
                { errorLogger.trackError(it) })
            .autoClear()

        resultListUpdateThrottle.debounce(800, TimeUnit.MILLISECONDS)
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { customAdapter.notifyDataSetChanged() },
                { errorLogger.trackError(it) })
            .autoClear()
    }

    fun onStartLocationClicked() {
        _startLocationListener.value = true // Notify View
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
            fromName.value = displayName
            fromContentDescription.value = "From $displayName"
        }
        _query.toLocation?.let {
            val displayName = it.displayName
            toName.value = displayName
            toContentDescription.value = "Going to $displayName"
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
                transportModeFilter!!.useTransportMode(it.id.orEmpty())
            }
            .map { mode ->
                tripResultTransportItemViewModelProvider.get().apply {
                    this.setup(mode)
                }
            }
            .map { viewModel ->
                viewModel.checked.value =
                    transportVisibilityFilter!!.isSelected(viewModel.modeId.value!!)
                viewModel
            }
            .map {
                it.clicked
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWithErrorHandling { type ->
                        // The transportVisibilityFilter will save walking vs wheelchair automatically,
                        // but we need to manually fix the display, as walking and wheelchair are mutually exclusive.
                        if (type.first == TransportMode.ID_WALK) {
                            toggleTransportModeChecked(TransportMode.ID_WHEEL_CHAIR, !type.second)
                        } else if (type.first == TransportMode.ID_WHEEL_CHAIR) {
                            toggleTransportModeChecked(TransportMode.ID_WALK, !type.second)
                        }

                        transportVisibilityFilter!!.setSelected(type.first, type.second)
                        reload()
                    }.autoClear()
                it
            }
            .toList()
            .subscribe({ list ->
                list.forEach {
                    println("tag123, modeiconId: ${it.modeIconId.value}")
                    println("tag123, modeId: ${it.modeId.value}")
                }
                transportModes.value = list
                if (execute) {
                    load()
                }
            }, {
                Timber.e(it)
                handleError(it)
            })
            .autoClear()
    }

    private fun toggleTransportModeChecked(mode: String, checked: Boolean) {
        transportModes.value?.forEach { model ->
            if (model.modeId.value == mode) {
                model.checked.value = checked
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
                            timeLabel.value = str
                        }, { error ->
                            isError.set(true)
                            handleError(error)
                            Timber.e(error, "An error in routing occurred ${error.message}")
                        }).autoClear()


                }
            } catch (_: Exception) {
            }
        }
    }

    private fun handleError(error: Throwable) {
        if (error.message.isNullOrBlank()) {
            onError.accept(context.getString(R.string.unknown_error))
        } else if(error.message == LOCATION_NOT_SUPPORTED_ERROR) {
            val fromLocation = query.fromLocation?.displayName.toString()
            val toLocation = query.toLocation?.displayName.toString()
            onError.accept(
                context.getString(
                    R.string.route_not_supported,
                    fromLocation,
                    toLocation
                )
            )
        } else {
            onError.accept(error.message)
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
                networkRequests.add(
                    routingStatusRepositoryLazy.get().putRoutingStatus(
                        RoutingStatus(
                            query.uuid(),
                            Status.InProgress()
                        )
                    ).subscribe()
                )
                loadFromStore()
            }.doOnError {
                val message = when (it) {
                    is RoutingError -> it.message
                    else -> context.getString(R.string.error_encountered)
                }
                networkRequests.add(
                    routingStatusRepositoryLazy.get().putRoutingStatus(
                        RoutingStatus(
                            query.uuid(),
                            Status.Error(message)
                        )
                    ).subscribe()
                )
            }
            .doOnComplete {
                networkRequests.add(
                    routingStatusRepositoryLazy.get().putRoutingStatus(
                        RoutingStatus(
                            query.uuid(),
                            Status.Completed()
                        )
                    ).subscribe()
                )
            }
            .doFinally {
                onFinished.accept(true)
                setLoading(false)
            }.subscribe({}, { error ->
                isError.set(true)
                handleError(error)
                Timber.e(error, "An error in routing occurred ${error.message}")
            })

        networkRequests.add(request)
        request.autoClear()
    }

    fun reload() {
        tripGroupRepository.clearPastRoutesAsync()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                proceedReload()
            }, {
                proceedReload()
            }).autoClear()
    }

    private fun proceedReload() {
        networkRequests.clear()
        updateResultList(emptyList())
        load()
    }

    private fun loadFromStore() {
        val tripFlow = MutableSharedFlow<Trip>()
        tripFlow.onEach {
            val clickEvent = ViewTrip(
                query = this.query,
                tripGroupUUID = it.group?.uuid().orEmpty(),
                sortOrder = 1, /* TODO Proper sorting */
                displayTripID = it.tripId
            )
            onItemClicked.accept(clickEvent)
        }.launchIn(viewModelScope)

        val quickBookingActionFlow = MutableSharedFlow<TripSegment>()
        quickBookingActionFlow.onEach {
            onQuickBookingActionClicked.accept(it)
        }.launchIn(viewModelScope)

        val requestDisposable = getSortedTripGroupsWithRoutingStatusProvider.get()
            .execute(query, 1, transportVisibilityFilter!!)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                val list = it.first

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
                        this.quickBookingActionClickFlow = quickBookingActionFlow
                        this.setTripGroup(context, group, classifier.classify(group))
                        onMoreButtonClicked.observable
                            .subscribeWithErrorHandling {
                                if (it.otherTripGroups.isNullOrEmpty()) {
                                    actionButtonHandler?.primaryActionClicked(it.trip)
                                } else {
                                    it.toggleShowMore()
                                }
                            }.autoClear()
                    }

                    vm
                }.sortedBy { it.group.trips?.minOf { it.weightedScore } }
            }
            .map {
                Pair(it, results.calculateDiff(it))
            }
            .subscribeWithErrorHandling {
                updateResultList(it.first, it.second)
                if (results.isEmpty() && !mergedList.contains(loadingItem) && !isError.get()) {
                    stateChange.accept(MultiStateView.ViewState.EMPTY)
                }
            }
        networkRequests.add(requestDisposable)
    }

    fun changeQuery(newQuery: Query) {
        updateResultList(emptyList())
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
        query.timeTag = timeTag
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

    fun updateTripGroup(updatedTripGroup: TripGroup) {
        var indexToUpdate = -1
        results.forEachIndexed { index, item ->
            if (item.group.uuid() == updatedTripGroup.uuid()) {
                indexToUpdate = index
                return@forEachIndexed
            }
        }

        if (indexToUpdate != -1) {

            val currentList = results.map { it }.toMutableList()
            currentList[indexToUpdate].setTripGroup(context, updatedTripGroup, null)
            val updatedItem = currentList[indexToUpdate]
            currentList.removeAt(indexToUpdate)
            currentList.add(indexToUpdate, updatedItem)

            updateResultList(currentList)
        }
    }

    fun setHelpInfoVisibility(show: Boolean) {
        _helpInfoVisible.postValue(show)
    }

    fun onShowBookARideInduction(show: Boolean) {
        _showHelpInfo.postValue(show)
    }

    private var updateJob: Job? = null

    private fun updateResultList(
        list: List<TripResultViewModel>,
        diffResult: DiffUtil.DiffResult? = null
    ) {
        diffResult?.let {
            results.update(list, it)
        } ?: run {
            results.update(list)
        }

        // Cancel the previous job if it's still active
        updateJob?.cancel()

        // Launch a new job with a debounce delay
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            customAdapter.notifyDataSetChanged()
        }
    }

}
