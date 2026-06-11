package com.skedgo.tripkit.ui.tripresult


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.booking.quickbooking.QuickBookingRepository
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Availability
import com.skedgo.tripkit.routing.Availability.Cancelled
import com.skedgo.tripkit.routing.GetOffAlertCache
import com.skedgo.tripkit.routing.MessageType
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripKitMapTiles
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.Visibilities
import com.skedgo.tripkit.routing.dateTimeZone
import com.skedgo.tripkit.routing.endDateTime
import com.skedgo.tripkit.routing.startDateTime
import com.skedgo.tripkit.routing.timetableEndDateTime
import com.skedgo.tripkit.routing.timetableStartDateTime
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.UiRenderToggles
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.creditsources.CreditSourcesOfDataViewModel
import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummaryItemViewModel
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonContainer
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.skedgo.tripkit.ui.utils.TripSegmentActionProcessor
import com.skedgo.tripkit.ui.utils.createSummaryIcon
import com.skedgo.tripkit.ui.utils.generateTripPreviewHeader
import com.skedgo.tripkit.ui.utils.getSegmentIconObservable
import com.squareup.otto.Bus
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import org.joda.time.DateTime
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections.emptyList
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

@SuppressLint("StaticFieldLeak")
class TripSegmentsViewModel @Inject internal constructor(
    private val context: Context,
    private val printTime: PrintTime,
    private val segmentViewModelProvider: Provider<TripSegmentItemViewModel>,
    private val creditSourcesOfDataViewModelProvider: Provider<CreditSourcesOfDataViewModel>,
    private val updateTripForRealtime: UpdateTripForRealtime,
    private val tripGroupRepository: TripGroupRepository,
    private val tripSegmentActionProcessor: TripSegmentActionProcessor,
    private val getAlternativeTripForAlternativeService: GetAlternativeTripForAlternativeService,
    private val tripUpdater: TripUpdater,
    private val remindersRepository: RemindersRepository,
    private val getTransportIconTintStrategy: GetTransportIconTintStrategy,
    private val quickBookingRepository: QuickBookingRepository
) : RxViewModel(), ActionButtonContainer, ActionButtonClickListener {

    companion object {
        const val TRIP_SUMMARY_DEBOUNCE = 500L
        
        // State saving constants for action buttons
        const val KEY_ACTION_BUTTONS_STATE = "action_buttons_state"
        const val KEY_ACTION_BUTTON_COUNT = "action_button_count"
        const val KEY_ACTION_BUTTON_TAG = "action_button_tag_"
        const val KEY_ACTION_BUTTON_TEXT = "action_button_text_"
        const val KEY_ACTION_BUTTON_ICON = "action_button_icon_"
        const val KEY_ACTION_BUTTON_IS_PRIMARY = "action_button_is_primary_"
        const val KEY_ACTION_BUTTON_USE_ICON_TINT = "action_button_use_icon_tint_"
        const val KEY_ACTION_BUTTON_FAVORITE_STATE = "action_button_favorite_state_"
        const val KEY_ACTION_BUTTON_ALERT_STATE = "action_button_alert_state_"
        const val KEY_IS_RESTORING_STATE = "is_restoring_state"
    }

    // State restoration flag to prevent conflicts during restoration
    private var isRestoringState = false

    private val segmentViewModels: MutableList<TripSegmentItemViewModel> = mutableListOf()
    val buttons = MutableLiveData<MutableList<ActionButtonViewModel>>(mutableListOf())
    val buttonsBinding: ItemBinding<ActionButtonViewModel> by lazy {
        ItemBinding.of<ActionButtonViewModel>(
            BR.viewModel,
            if (UiRenderToggles.useComposeActionButtonUi) {
                R.layout.trip_segment_action_button
            } else {
                R.layout.trip_segment_action_button_xml
            }
        )
            .bindExtra(BR.listener, this)
    }

    val itemViewModels = ObservableField(emptyList<Any>())

    /**
     * Getting error on unit test when initializing viewModel class with BR for
     * me.tatarka.bindingcollectionadapter2.ItemBinding, so updated the implementation to
     * use lazy to not execute me.tatarka.bindingcollectionadapter2.ItemBinding on viewModel
     * initialization
     */
    val itemBinding: ItemBinding<Any> by lazy {
        ItemBinding.of(
            OnItemBindClass<Any>()
                .map(
                    CreditSourcesOfDataViewModel::class.java,
                    BR.viewModel,
                    R.layout.credit_sources_of_data
                )
                .map(TripSegmentItemViewModel::class.java, BR.viewModel, R.layout.trip_segment)
                .map(
                    TripSegmentGetOffAlertsViewModel::class.java,
                    BR.viewModel,
                    R.layout.trip_segment_get_off_alert
                )
        )
    }
    val showCloseButton = MutableLiveData(false)
    val isHideExactTimes = MutableLiveData(false)
    val isCancelled = MutableLiveData(false)

    internal val onStreetViewTapped = PublishSubject.create<Location>()
    private val creditSourcesOfDataViewModel = BehaviorRelay.create<CreditSourcesOfDataViewModel>()
    private val tripGroupRelay = BehaviorRelay.create<TripGroup>()

    internal var itemsChangeEmitter = PublishSubject.create<List<TripSegmentItemViewModel>>()
    internal var bookingForm: BookingForm? = null
    private var internalBus: Bus? = null
    val alertsClicked = PublishRelay.create<ArrayList<RealtimeAlert>>()
    val segmentClicked = PublishRelay.create<TripSegment>()
    val externalActionClicked = PublishRelay.create<TripSegment>()
    val ticketInfoClicked = PublishRelay.create<String>()
    var durationTitle = MutableLiveData<String>()
    var arriveAtTitle = MutableLiveData<String>()
    val locationLabel = PublishRelay.create<String>()
    private var actionButtonHandler: ActionButtonHandler? = null
    private var trip: Trip? = null

    val customAdapter = TripSegmentCustomRecyclerViewAdapter<Any>()

    private val _mapTiles = MutableLiveData<TripKitMapTiles>()
    val mapTiles: LiveData<TripKitMapTiles> = _mapTiles

    private val _updatedState = MutableLiveData<Unit>()
    val updatedState: LiveData<Unit> = _updatedState

    val tripGroupObservable: Observable<TripGroup>
        get() = tripGroupRelay.hide()

    private val _geofenceCircles = MutableLiveData<List<Pair<LatLng, Double>>>()
    val geofenceCircles: LiveData<List<Pair<LatLng, Double>>> = _geofenceCircles

    val timeZoneId: String
        get() {
            val value = tripGroupRelay.value
            val displayTrip = value?.displayTrip
            val timeZone = displayTrip!!.from?.dateTimeZone
            return timeZone?.toTimeZone()?.id ?: TimeZone.getDefault().id
        }

    var tripGroup: TripGroup
        get() = tripGroupRelay.value!!
        internal set(tripGroup) = setTripGroup(tripGroup, -1, null)

    private val _userLocation = MutableLiveData<android.location.Location>()
    val userLocation: LiveData<android.location.Location> = _userLocation

    private var tripSegmentGetOffAlertsViewModel: TripSegmentGetOffAlertsViewModel? = null

    private val tripSummaryStream = MutableSharedFlow<List<TripSegmentSummaryItemViewModel>>()

    val summaryItems: MutableLiveData<MutableList<TripSegmentSummaryItemViewModel>> =
        MutableLiveData(mutableListOf())
    val summaryItemsBinding: ItemBinding<TripSegmentSummaryItemViewModel> by lazy {
        ItemBinding.of<TripSegmentSummaryItemViewModel>(
            BR.viewModel,
            R.layout.item_trip_segment_summary
        )
    }

    var tripAlertChangeValidator: (() -> Boolean)? = null

    init {
        tripSummaryStream
            .debounce(TRIP_SUMMARY_DEBOUNCE)
            .flowOn(Dispatchers.IO)
            .onEach { tripSummaryItems ->
                summaryItems.value = tripSummaryItems.sortedBy { it.id.value }.toMutableList()
            }
            .catch { exception ->
                Timber.e(exception)
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    fun setActionButtonHandlerFactory(
        actionButtonHandlerFactory: ActionButtonHandlerFactory?,
        queryFromLocation: Location?,
        queryToLocation: Location?
    ) {
        actionButtonHandler = actionButtonHandlerFactory?.createHandler(this)
        actionButtonHandler?.queryFromLocation = queryFromLocation
        actionButtonHandler?.queryToLocation = queryToLocation
        
        // If we have a trip group and action button handler is now available, setup buttons
        if (actionButtonHandler != null && tripGroupRelay.hasValue()) {
            setupButtons(tripGroupRelay.value!!)
        }
    }

    fun setInternalBus(bus: Bus) {
        this.internalBus = bus
    }

    fun loadTripGroup(
        tripGroupId: String,
        tripId: Long,
        savedInstanceState: Bundle?,
        displayTripId: Int? = null
    ) {
        tripGroupRepository.getTripGroup(tripGroupId)
            .observeOn(mainThread())
            .onErrorResumeNext(Observable.empty())
            .doOnError { it.printStackTrace() }
            .subscribe(
                { tripGroup ->
                    /*
                    //=== For testing isHideExactTimes purpose only while API is not yet updated ===
                    tripGroup.trips?.forEach { trip ->
                        trip.getSummarySegments().forEach { segment ->
                            if (segment.transportModeId == TransportMode.ID_WALK) {
                                segment.isHideExactTimes = true
                            }
                        }
                    }
                    // ==
                    */

                    if (tripId != -1L) {
                        tripGroup.displayTripId = tripId
                    }
                    setTitleAndSubtitle(tripGroup, tripId)
                    setTripGroup(tripGroup, tripId, savedInstanceState)
                    setupButtons(tripGroup)
                }, { it.printStackTrace() }
            )
            .autoClear()
    }

    /**
     * Creates or updates action buttons for the current display trip.
     *
     * During state restoration we may already have placeholder button view models from
     * [restoreActionButtonState]. If the restored button count differs from the latest handler
     * output, we rebuild the button list to avoid index-based mismatches and keep UI state valid.
     * This guarantees we never read outside list bounds when action availability changes between
     * save and restore events.
     */
    private fun setupButtons(tripGroup: TripGroup) {
        if (tripGroup.displayTrip == null) return
        
        viewModelScope.launch {
            if (actionButtonHandler == null) {
                return@launch
            }
            
            val handler = actionButtonHandler!!
            val actions = handler.getActions(context, tripGroup.displayTrip!!).distinctBy { it.text }
            
            // If we're restoring state and buttons are already restored, update them with proper data
            if (isRestoringState && buttons.value?.isNotEmpty() == true) {
                val existingButtons = buttons.value.orEmpty()
                if (existingButtons.size == actions.size) {
                    updateButtonsPreservingDynamicStates(actions)
                } else {
                    recreateButtons(actions)
                }
                isRestoringState = false
            } else {
                // Create new buttons or update existing ones
                if (buttons.value.orEmpty().size != actions.size) {
                    recreateButtons(actions)
                } else {
                    updateButtonsPreservingDynamicStates(actions)
                }
            }
        }
    }

    /**
     * Recreates button view models from handler output.
     */
    private fun recreateButtons(actions: List<ActionButton>) {
        val newButtons = mutableListOf<ActionButtonViewModel>()
        actions.forEach { actionButton ->
            val buttonViewModel = ActionButtonViewModel(context, actionButton)
            newButtons.add(buttonViewModel)
        }
        buttons.value = newButtons
    }

    /**
     * Updates existing button view models while preserving dynamic text-based states
     * (favorite and alert toggles).
     */
    private fun updateButtonsPreservingDynamicStates(actions: List<ActionButton>) {
        val existingButtons = buttons.value.orEmpty()
        actions.forEachIndexed { i, actionButton ->
            val existingButton = existingButtons.getOrNull(i) ?: return@forEachIndexed
            // Update with latest action metadata first.
            existingButton.update(context, actionButton)

            // Reapply dynamic states derived from current UI text.
            when (actionButton.tag) {
                ActionButtonHandler.ACTION_TAG_FAVORITE -> {
                    val currentText = existingButton.title.get()
                    if (currentText?.contains("Remove", ignoreCase = true) == true) {
                        existingButton.title.set(context.getString(R.string.remove_favourite))
                    }
                }
                ActionButtonHandler.ACTION_TAG_ALERT -> {
                    val currentText = existingButton.title.get()
                    if (currentText?.contains("Mute", ignoreCase = true) == true) {
                        existingButton.title.set(context.getString(R.string.action_mute))
                    }
                }
            }
        }
    }

    private fun setTitleAndSubtitle(tripGroup: TripGroup, tripId: Long) {
        val trip = tripGroup.trips?.firstOrNull { it.tripId == tripId } ?: tripGroup.displayTrip
        if (trip == null || trip.from == null || trip.to == null) return
        if (trip.isDepartureTimeFixed()) {
            durationTitle.value =
                "${printTime.print(trip.startDateTime)} - ${printTime.print(trip.endDateTime)}"
            arriveAtTitle.value =
                formatDuration(context, trip.startTimeInSecs, trip.endTimeInSecs)
        } else {
            durationTitle.value =
                formatDuration(context, trip.startTimeInSecs, trip.endTimeInSecs)
            if (!trip.queryIsLeaveAfter) {
                arriveAtTitle.value =
                    context.resources.getString(
                        R.string.departs__pattern,
                        printTime.print(trip.startDateTime)
                    ).capitalize()
            } else {
                arriveAtTitle.value =
                    context.resources.getString(
                        R.string.arrives__pattern,
                        printTime.print(trip.endDateTime)
                    ).capitalize()
            }
        }

        isHideExactTimes.value =
            trip.hideExactTimes || trip.segmentList.any { it.isHideExactTimes }
    }
    // TODO This function is duplicated in TripResultViewModel
    /**
     * For example, 1hr 50mins
     */
    private fun formatDuration(
        context: Context,
        startTimeInSecs: Long,
        endTimeInSecs: Long
    ): String =
        TimeUtils.getDurationInDaysHoursMins(context, (endTimeInSecs - startTimeInSecs).toInt())


    fun findSegmentPosition(tripSegment: TripSegment): Int {
        for (i in segmentViewModels.indices) {
            val viewModel = segmentViewModels[i]
            val segment = viewModel.tripSegment
            if (segment?.segmentId == tripSegment.segmentId) {
                return i
            }
        }
        return -1
    }

    fun onStart() {
        startUpdate()
    }

    fun onStop() {
        updateTripForRealtime.stop()
    }

    override fun onCleared() {
        super.onCleared()
        if (segmentViewModels.isNotEmpty()) {
            for (i in segmentViewModels.indices) {
                val itemViewModel = segmentViewModels[i]
                itemViewModel.onCleared()
            }
        }
    }


    private fun processedText(segment: TripSegment, text: String?): String {
        return if (!text.isNullOrBlank()) {
            tripSegmentActionProcessor.processText(context, segment, text, false)
        } else {
            String()
        }
    }

    private fun addTerminalItem(
        viewModel: TripSegmentItemViewModel,
        tripSegment: TripSegment,
        previousSegment: TripSegment? = null,
        nextSegment: TripSegment? = null
    ) {
        val time = when (tripSegment.getType()) {
            SegmentType.DEPARTURE -> printTime.print(tripSegment.startDateTime)
            SegmentType.ARRIVAL -> printTime.print(tripSegment.endDateTime)
            else -> null
        }

        var topConnectionColor: Int = Color.TRANSPARENT
        var bottomConnectionColor: Int = Color.TRANSPARENT

        val connectionColor = if (tripSegment.getType() == SegmentType.DEPARTURE) {
            nextSegment?.lineColor() ?: Color.TRANSPARENT
        } else {
            previousSegment?.lineColor() ?: Color.TRANSPARENT
        }

        if (tripSegment.getType() == SegmentType.DEPARTURE) {
            bottomConnectionColor = connectionColor
        }
        if (tripSegment.getType() == SegmentType.ARRIVAL) {
            topConnectionColor = connectionColor
        }

        viewModel.setupSegment(
            viewType = TripSegmentItemViewModel.SegmentViewType.TERMINAL,
            title = processedText(tripSegment, tripSegment.action),
            startTime = time,
            lineColor = connectionColor,
            topConnectionColor = topConnectionColor,
            bottomConnectionColor = bottomConnectionColor,
            isCancelled = tripSegment.availability.equals(Cancelled.value, ignoreCase = true)
        )
    }

    private fun addStationaryItem(
        viewModel: TripSegmentItemViewModel,
        tripSegment: TripSegment,
        previousSegment: TripSegment? = null,
        nextSegment: TripSegment? = null
    ) {
        var startTime: String? = when {
            nextSegment != null -> printTime.print(nextSegment.startDateTime)
            else -> null
        }

        var endTime: String? = null
        var delay = 0L

        nextSegment?.let {
            if (it.isRealTime && it.timetableStartTime > 0) {
                delay = it.startTimeInSecs - it.timetableStartTime
                if (delay != 0L) {
                    startTime = printTime.print(it.timetableStartDateTime)
                }

                endTime = when {
                    delay == 0L -> null
                    else -> printTime.print(it.timetableEndDateTime)
                }
            }
        }

        val possibleDescription = when {
            !nextSegment?.platform.isNullOrBlank() ->
                context.getString(
                    R.string.platform,
                    nextSegment!!.platform!!.replace("Platform", "", true)
                )

            !tripSegment.action.isNullOrBlank() -> processedText(tripSegment, tripSegment.action)
            else -> null
        }

        var location = context.resources.getString(R.string.location)
        if (tripSegment.singleLocation != null && !tripSegment.singleLocation?.address.isNullOrEmpty()) {
            location = tripSegment.singleLocation?.address ?: tripSegment.singleLocation?.displayAddress.orEmpty()
        }
        if (!tripSegment.sharedVehicle?.garage()?.address.isNullOrEmpty()) {
            location = tripSegment.sharedVehicle?.garage()?.address!!
        }
        if (!nextSegment?.from?.address.isNullOrEmpty()) {
            location = nextSegment?.from?.address!!
        } else if (!previousSegment?.from?.address.isNullOrEmpty()) {
            location = previousSegment?.from?.address!!
        }

        viewModel.setupSegment(
            viewType = TripSegmentItemViewModel.SegmentViewType.STATIONARY,
            title = location,
            description = possibleDescription,
            notes = tripSegment.getDisplayNotes(context, false),
            startTime = startTime,
            endTime = endTime,
            delay = delay,
            hasRealtime = nextSegment?.isRealTime ?: false,
            topConnectionColor = previousSegment?.lineColor() ?: Color.TRANSPARENT,
            bottomConnectionColor = nextSegment?.lineColor() ?: Color.TRANSPARENT
        )
    }

    private fun addStationaryBridgeItem(
        viewModel: TripSegmentItemViewModel,
        tripSegment: TripSegment,
        nextSegment: TripSegment? = null
    ) {

        val possibleTitle = nextSegment?.from?.displayName ?: tripSegment.to?.displayName

        val possibleDescription = when {
            !nextSegment?.platform.isNullOrBlank() ->
                context.getString(
                    R.string.platform,
                    nextSegment!!.platform!!.replace("Platform", "", true)
                )

            else -> null
        }

        // If nextSegment is null, the bridge is the end point, otherwise it's the start
        var endTime = if (nextSegment != null) printTime.print(nextSegment.startDateTime) else null

        // If it's a real-time service, we show the time in green if it's on-time, yellow if it's early, and red if it's late.
        // In both cases, we show the original time crossed out below the current real-time information.
        var delay = 0L

        val realtimeTripSegment = when {
            tripSegment.isRealTime -> tripSegment
            (nextSegment != null) && nextSegment.isRealTime -> nextSegment
            else -> null
        }

        realtimeTripSegment?.let {
            if (!tripSegment.isRealTime && it.timetableStartTime > 0) {
                // The starting stationary bridge shows the departure time
                delay = it.startTimeInSecs - it.timetableStartTime
                endTime = when {
                    delay == 0L -> null
                    else -> printTime.print(it.timetableStartDateTime)
                }
            } else if (it.timetableEndTime > 0) {
                // The end stationary bridge shows the arrival time
                delay = it.endTimeInSecs - it.timetableEndTime
                endTime = when {
                    delay == 0L -> null
                    else -> printTime.print(it.timetableEndDateTime)
                }
            } else {
                endTime = null
            }
        }

        viewModel.setupSegment(
            viewType = TripSegmentItemViewModel.SegmentViewType.STATIONARY_BRIDGE,
            title = possibleTitle ?: context.resources.getString(R.string.location),
            description = possibleDescription,
            notes = tripSegment.getDisplayNotes(context, false),
            startTime = printTime.print(tripSegment.endDateTime),
            endTime = endTime,
            delay = delay,
            hasRealtime = (realtimeTripSegment != null),
            topConnectionColor = tripSegment.lineColor(),
            bottomConnectionColor = nextSegment?.lineColor() ?: Color.TRANSPARENT,
            isStationaryItem = true,
            isCancelled = tripSegment.availability.equals(Cancelled.value, ignoreCase = true)
        )
    }

    private fun addMovingItem(
        viewModel: TripSegmentItemViewModel,
        tripSegment: TripSegment
    ) {

        viewModel.setupSegment(
            viewType = TripSegmentItemViewModel.SegmentViewType.MOVING,
            title = processedText(tripSegment, tripSegment.action),
            description = tripSegment.getDisplayNotes(context, false),
            lineColor = tripSegment.lineColor(),
            isCancelled = tripSegment.availability.equals(Cancelled.value, ignoreCase = true)
        )
    }

    private fun setTripGroup(tripGroup: TripGroup, tripId: Long, savedInstanceState: Bundle?) {
        tripGroupRelay.accept(tripGroup)
        val newItems = ArrayList<Any>()
        val trip = tripGroup.trips?.firstOrNull { it.tripId == tripId } ?: tripGroup.displayTrip
        if (trip != null) {
            this.trip = trip
            isCancelled.value = trip.getAvailability() == Cancelled
            val tripSegments = trip.segmentList
            segmentViewModels.clear()

            _mapTiles.postValue(tripSegments.firstOrNull { it.mapTiles != null }?.mapTiles)

            generateSummaryItems(
                tripSegments.filter { it.visibility == Visibilities.VISIBILITY_IN_SUMMARY }
            )

            tripSegments.forEachIndexed { index, segment ->
                val previousSegment = tripSegments.elementAtOrNull(index - 1)
                val nextSegment = tripSegments.elementAtOrNull(index + 1)

                val viewModel = segmentViewModelProvider.get()
                viewModel.alertsClicked.subscribeWithErrorHandling {
                    alertsClicked.accept(it)
                }.autoClear()

                viewModel.externalActionClicked.subscribeWithErrorHandling {
                    externalActionClicked.accept(it)
                }.autoClear()

                viewModel.onClick.observable.subscribeWithErrorHandling {
                    it.tripSegment?.let { segment ->
                        segmentClicked.accept(segment)
                    }
                }.autoClear()

                viewModel.onTicketInfoClicked.observable.subscribeWithErrorHandling {
                    it.tripSegment?.ticketURL.let { ticketUrl ->
                        ticketInfoClicked.accept(ticketUrl)
                    }
                }.autoClear()

                viewModel.tripSegment = segment

                if (segment.getType() == SegmentType.ARRIVAL || segment.getType() == SegmentType.DEPARTURE) {
                    addTerminalItem(viewModel, segment, previousSegment, nextSegment)
                } else if (segment.isStationary && ((segment.getType() == null && segment.startStopCode == null) || segment.getType() == SegmentType.STATIONARY)) {
                    addStationaryItem(viewModel, segment, previousSegment, nextSegment)
                } else {
                    if (nextSegment != null && !nextSegment.isStationary && nextSegment.getType() != SegmentType.ARRIVAL) {
                        val bridgeModel = segmentViewModelProvider.get()
                        bridgeModel.tripSegment = segment
                        addMovingItem(bridgeModel, segment)
                        bridgeModel.alertsClicked.subscribeWithErrorHandling {
                            alertsClicked.accept(it)
                        }.autoClear()

                        bridgeModel.externalActionClicked.subscribeWithErrorHandling {
                            externalActionClicked.accept(it)
                        }.autoClear()

                        bridgeModel.onClick.observable.subscribeWithErrorHandling {
                            it.tripSegment?.let { segment ->
                                segmentClicked.accept(segment)
                            }
                        }.autoClear()
                        bridgeModel.onTicketInfoClicked.observable.subscribeWithErrorHandling {
                            it.tripSegment?.ticketURL.let { ticketUrl ->
                                ticketInfoClicked.accept(ticketUrl)
                            }
                        }.autoClear()
                        segmentViewModels.add(bridgeModel)

                        addStationaryBridgeItem(viewModel, segment, nextSegment)
                    } else {
                        addMovingItem(viewModel, segment)
                    }
                }
                segmentViewModels.add(viewModel)
            }
            newItems.addAll(segmentViewModels)
            itemsChangeEmitter.onNext(segmentViewModels)
        }

        trip?.let {
            setupGetOffAlert(tripGroup, it)?.let { viewModel ->
                tripSegmentGetOffAlertsViewModel = viewModel
                newItems.add(viewModel)
            }
        }

        if (tripGroup.sources != null && tripGroup.sources!!.size > 0) {
            val creditSourcesOfDataViewModel = creditSourcesOfDataViewModelProvider.get()
            creditSourcesOfDataViewModel.changeSources(tripGroup.sources!!)
            newItems.add(creditSourcesOfDataViewModel)
            this.creditSourcesOfDataViewModel.accept(creditSourcesOfDataViewModel)
        }

        itemViewModels.set(newItems)
    }

    @VisibleForTesting
    fun generateSummaryItems(segments: List<TripSegment>) {
        val tripSegmentSummaryItem =
            mutableListOf<TripSegmentSummaryItemViewModel>()
        segments.forEach { segment ->
            segment.getSegmentIconObservable(
                context, getTransportIconTintStrategy
            ).map { bitmapDrawable ->
                segment.createSummaryIcon(context, bitmapDrawable)
            }.subscribe({ drawable ->
                if (tripSegmentSummaryItem.none { it.id.value == segment.segmentId }) {
                    tripSegmentSummaryItem.add(
                        segment.generateTripPreviewHeader(context, drawable, printTime)
                            .getSummaryItem()
                    )
                }

                viewModelScope.launch {
                    tripSummaryStream.emit(tripSegmentSummaryItem)
                }
            }, {
                Timber.e(it)
            }).autoClear()
        }
    }

    private fun setupGetOffAlert(
        tripGroup: TripGroup,
        trip: Trip
    ): TripSegmentGetOffAlertsViewModel? {
        try {
            val isOn = GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())

            val getOffAlertsViewModel =
                TripSegmentGetOffAlertsViewModel(trip, isOn, tripUpdater, remindersRepository)

            getOffAlertsViewModel.alertStateToggleCustomValidation = { context, isOn ->
                // Only validate when enabling alerts; disabling/reset should always proceed.
                if (!isOn) {
                    getOffAlertsViewModel.onAlertChange(context, isOn)
                } else {
                    // null tripAlertChangeValidator means no validations required
                    if (tripAlertChangeValidator == null || tripAlertChangeValidator?.invoke() == true) {
                        getOffAlertsViewModel.onAlertChange(context, isOn)
                    } else {
                        getOffAlertsViewModel.setGetOffAlertStateOn(false)
                    }
                }
            }

            getOffAlertsViewModel.showGeofencesOnMap = { _geofenceCircles.postValue(it) }

            getOffAlertsViewModel.alertStateListener = {
                setupButtons(tripGroup)
                _updatedState.postValue(Unit)
            }
            val messageTypes =
                trip.segmentList.flatMap { it.geofences.orEmpty() }.map { it.messageType }

            val startSegmentStartTimeInSecs =
                trip.segmentList?.minByOrNull { it.startTimeInSecs }?.startTimeInSecs ?: 0

            val reminderInMinutes =
                runBlocking { remindersRepository.getTripNotificationReminderMinutes() }

            val reminder = TimeUnit.MINUTES.toSeconds(reminderInMinutes)

            val currentDateTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(
                DateTime(System.currentTimeMillis(), trip.from?.dateTimeZone).millis
            )

            val isAboutToStart = if (startSegmentStartTimeInSecs > currentDateTimeInSeconds) {
                (startSegmentStartTimeInSecs - currentDateTimeInSeconds) >= reminder
            } else {
                false
            }

            getOffAlertsViewModel.setup(
                context,
                listOf(
                    TripSegmentGetOffAlertDetailViewModel(
                        ContextCompat.getDrawable(context, R.drawable.ic_navigation_start),
                        context.getString(R.string.get_off_alerts_trip_about_to_start),
                        isAboutToStart
                    ), TripSegmentGetOffAlertDetailViewModel(
                        ContextCompat.getDrawable(context, R.drawable.ic_navigation_near),
                        context.getString(R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop),
                        messageTypes.any { it == MessageType.VEHICLE_IS_APPROACHING.name }
                            || trip.subscribeURL.isNullOrBlank().not()
                    ),
                    TripSegmentGetOffAlertDetailViewModel(
                        ContextCompat.getDrawable(context, R.drawable.ic_navigation_near),
                        context.getString(R.string.get_off_alerts_getting_within_disembarkation_point),
                        messageTypes.any { it == MessageType.ARRIVING_AT_YOUR_STOP.name }
                    ),

                    TripSegmentGetOffAlertDetailViewModel(
                        ContextCompat.getDrawable(context, R.drawable.ic_navigation_near),
                        context.getString(R.string.get_off_alerts_passed_by_the_previous_stop),
                        messageTypes.any { it == MessageType.NEXT_STOP_IS_YOURS.name }
                    ),
                    TripSegmentGetOffAlertDetailViewModel(
                        ContextCompat.getDrawable(context, R.drawable.ic_final_destination),
                        context.getString(R.string.get_off_alerts_about_to_arrive_final_destination),
                        messageTypes.any { it == MessageType.TRIP_END.name }
                    )
                )
            )

            return getOffAlertsViewModel
        } catch (e: Exception) {
            return null
        }
    }

    fun validateGetOffAlerts() {
        tripSegmentGetOffAlertsViewModel?.validate()
    }

    private fun startUpdate() {
        updateTripForRealtime.start(tripGroupRelay.hide())
    }

    override fun scope(): CoroutineScope = viewModelScope
    override fun replaceTripGroup(tripGroupUuid: String, newTripGroup: TripGroup) {
        setupButtons(newTripGroup)
    }

    override fun onItemClick(tag: String, viewModel: ActionButtonViewModel, context: Context) {
        when (tag) {
            ActionButtonHandler.ACTION_TAG_ALERT -> {
                tripSegmentGetOffAlertsViewModel?.apply {
                    setAlertState(context, getOffAlertStateOn.value?.not() ?: false)
                }
                // Update button state after alert toggle
                updateButtonStateAfterAction(tag, viewModel, context)
            }
            ActionButtonHandler.ACTION_EXTERNAL_SHOW_TICKET -> {
                getTicket()
            }
            else -> {
                actionButtonHandler?.actionClicked(
                    context, tag, this.trip ?: tripGroup.displayTrip!!, viewModel
                )
                // Update button state after action
                updateButtonStateAfterAction(tag, viewModel, context)
            }
        }
    }

    /**
     * Update button state after user interaction
     */
    private fun updateButtonStateAfterAction(tag: String, viewModel: ActionButtonViewModel, context: Context) {
        when (tag) {
            ActionButtonHandler.ACTION_TAG_ALERT -> {
                val trip = this.trip ?: tripGroup.displayTrip
                if (trip != null) {
                    val isAlertOn = GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())
                    val newText = if (isAlertOn) {
                        context.getString(R.string.action_mute)
                    } else {
                        context.getString(R.string.action_alert_me)
                    }
                    viewModel.title.set(newText)
                }
            }
            // Add other dynamic button states as needed
        }
    }

    private fun getTicket() {
        viewModelScope.launch {
            quickBookingRepository.getTickets().collectLatest { result ->
                when (result) {
                    is com.skedgo.tripkit.utils.async.Result.Loading -> {
                        withContext(Dispatchers.Main) {
                            buttons.value?.firstOrNull {
                                it.tag == ActionButtonHandler.ACTION_EXTERNAL_SHOW_TICKET
                            }?.showSpinner(true)
                        }
                    }

                    is com.skedgo.tripkit.utils.async.Result.Success -> {
                        withContext(Dispatchers.Main) {
                            buttons.value?.firstOrNull {
                                it.tag == ActionButtonHandler.ACTION_EXTERNAL_SHOW_TICKET
                            }?.showSpinner(false)
                            val tickets = result.data

                            val formatter = DateTimeFormatter.ISO_DATE_TIME
                            tickets.maxByOrNull { ticket ->
                                // Parse the ticket expiration string to a LocalDateTime
                                ZonedDateTime.parse(ticket.ticketExpirationTimestamp, formatter)
                                    .toInstant().toEpochMilli()
                            }?.let { ticket ->
                                actionButtonHandler?.handleCustomAction(
                                    ActionButtonHandler.ACTION_EXTERNAL_SHOW_TICKET,
                                    ticket
                                )
                            }
                        }
                    }

                    is com.skedgo.tripkit.utils.async.Result.Error -> {
                        withContext(Dispatchers.Main) {
                            buttons.value?.firstOrNull {
                                it.tag == ActionButtonHandler.ACTION_EXTERNAL_SHOW_TICKET
                            }?.showSpinner(false)
                        }
                    }
                }
            }
        }
    }

    // MARK: - State Management Methods

    /**
     * Save the current state of action buttons to the provided Bundle
     */
    fun onSavedInstanceState(outState: Bundle) {
        saveActionButtonState(outState)
    }

    /**
     * Restore the state of action buttons from the provided Bundle
     */
    fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            isRestoringState = true
            restoreActionButtonState(savedInstanceState)
        }
    }

    /**
     * Save action button state to Bundle
     */
    private fun saveActionButtonState(outState: Bundle) {
        val currentButtons = buttons.value
        if (currentButtons.isNullOrEmpty()) {
            return
        }

        outState.putInt(KEY_ACTION_BUTTON_COUNT, currentButtons.size)
        
        currentButtons.forEachIndexed { index, buttonViewModel ->
            // Save basic button properties
            outState.putString("${KEY_ACTION_BUTTON_TAG}$index", buttonViewModel.tag)
            outState.putString("${KEY_ACTION_BUTTON_TEXT}$index", buttonViewModel.title.get())
            
            // Save dynamic states for specific button types
            when (buttonViewModel.tag) {
                ActionButtonHandler.ACTION_TAG_FAVORITE -> {
                    // Save favorite state (text indicates favorite/unfavorite)
                    val isFavorite = buttonViewModel.title.get()?.contains("Remove", ignoreCase = true) == true
                    outState.putBoolean("${KEY_ACTION_BUTTON_FAVORITE_STATE}$index", isFavorite)
                }
                ActionButtonHandler.ACTION_TAG_ALERT -> {
                    // Save alert state (text indicates alert/mute)
                    val isAlertOn = buttonViewModel.title.get()?.contains("Mute", ignoreCase = true) == true
                    outState.putBoolean("${KEY_ACTION_BUTTON_ALERT_STATE}$index", isAlertOn)
                }
            }
        }
        
        outState.putBoolean(KEY_IS_RESTORING_STATE, isRestoringState)
    }

    /**
     * Restore action button state from Bundle
     */
    private fun restoreActionButtonState(savedInstanceState: Bundle) {
        val buttonCount = savedInstanceState.getInt(KEY_ACTION_BUTTON_COUNT, 0)
        if (buttonCount <= 0) {
            return
        }

        val restoredButtons = mutableListOf<ActionButtonViewModel>()
        
        for (i in 0 until buttonCount) {
            val tag = savedInstanceState.getString("${KEY_ACTION_BUTTON_TAG}$i")
            val text = savedInstanceState.getString("${KEY_ACTION_BUTTON_TEXT}$i")
            
            if (tag != null && text != null) {
                // Create a temporary ActionButton with basic info
                // The full ActionButton will be recreated when setupButtons is called
                val tempButton = ActionButton(
                    text = text,
                    tag = tag,
                    icon = 0, // Will be set when ActionButton is recreated
                    isPrimary = false, // Will be set when ActionButton is recreated
                    useIconTint = true
                )
                
                val buttonViewModel = ActionButtonViewModel(context, tempButton)
                
                // Restore dynamic states for specific button types
                when (tag) {
                    ActionButtonHandler.ACTION_TAG_FAVORITE -> {
                        val isFavorite = savedInstanceState.getBoolean("${KEY_ACTION_BUTTON_FAVORITE_STATE}$i", false)
                        // Update text based on favorite state
                        val favoriteText = if (isFavorite) {
                            context.getString(R.string.remove_favourite)
                        } else {
                            context.getString(R.string.favourite)
                        }
                        buttonViewModel.title.set(favoriteText)
                    }
                    ActionButtonHandler.ACTION_TAG_ALERT -> {
                        val isAlertOn = savedInstanceState.getBoolean("${KEY_ACTION_BUTTON_ALERT_STATE}$i", false)
                        // Update text based on alert state
                        val alertText = if (isAlertOn) {
                            context.getString(R.string.action_mute)
                        } else {
                            context.getString(R.string.action_alert_me)
                        }
                        buttonViewModel.title.set(alertText)
                    }
                }
                
                restoredButtons.add(buttonViewModel)
            }
        }
        
        if (restoredButtons.isNotEmpty()) {
            buttons.value = restoredButtons
        }
    }

}
