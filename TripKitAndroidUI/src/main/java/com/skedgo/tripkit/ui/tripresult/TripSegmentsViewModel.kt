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
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.creditsources.CreditSourcesOfDataViewModel
import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummaryItemViewModel
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import org.joda.time.DateTime
import timber.log.Timber
import java.lang.Exception
import java.util.*
import java.util.Collections.emptyList
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
    private val getTransportIconTintStrategy: GetTransportIconTintStrategy
) : RxViewModel(), ActionButtonContainer, ActionButtonClickListener {

    companion object {
        const val TRIP_SUMMARY_DEBOUNCE = 500L
    }

    private val segmentViewModels: MutableList<TripSegmentItemViewModel> = ArrayList()
    val buttons = ObservableArrayList<ActionButtonViewModel>()
    val buttonsBinding: ItemBinding<ActionButtonViewModel> by lazy {
        ItemBinding.of<ActionButtonViewModel>(BR.viewModel, R.layout.trip_segment_action_button)
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
    val showCloseButton = ObservableBoolean(false)
    val isHideExactTimes = ObservableBoolean(false)

    internal val onStreetViewTapped = PublishSubject.create<Location>()
    private val creditSourcesOfDataViewModel = BehaviorRelay.create<CreditSourcesOfDataViewModel>()
    private val tripGroupRelay = BehaviorRelay.create<TripGroup>()

    internal var itemsChangeEmitter = PublishSubject.create<List<TripSegmentItemViewModel>>()
    internal var bookingForm: BookingForm? = null
    private var internalBus: Bus? = null
    val alertsClicked = PublishRelay.create<ArrayList<RealtimeAlert>>()
    val segmentClicked = PublishRelay.create<TripSegment>()
    val externalActionClicked = PublishRelay.create<TripSegment>()
    var durationTitle = ObservableField<String>()
    var arriveAtTitle = ObservableField<String>()
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

    val timeZoneId: String
        get() {
            val value = tripGroupRelay.value
            val displayTrip = value?.displayTrip
            val timeZone = displayTrip!!.from.dateTimeZone
            return timeZone.toTimeZone().id
        }

    var tripGroup: TripGroup
        get() = tripGroupRelay.value!!
        internal set(tripGroup) = setTripGroup(tripGroup, -1, null)

    private val _userLocation = MutableLiveData<android.location.Location>()
    val userLocation: LiveData<android.location.Location> = _userLocation

    private var tripSegmentGetOffAlertsViewModel: TripSegmentGetOffAlertsViewModel? = null

    private val tripSummaryStream = MutableSharedFlow<List<TripSegmentSummaryItemViewModel>>()

    val summaryItems: ObservableArrayList<TripSegmentSummaryItemViewModel> = ObservableArrayList()
    val summaryItemsBinding: ItemBinding<TripSegmentSummaryItemViewModel> by lazy {
        ItemBinding.of<TripSegmentSummaryItemViewModel>(
            BR.viewModel,
            R.layout.item_trip_segment_summary
        )
    }

    init {
        tripSummaryStream
            .debounce(TRIP_SUMMARY_DEBOUNCE)
            .flowOn(Dispatchers.IO)
            .onEach { tripSummaryItems ->
                summaryItems.clear()
                summaryItems.addAll(tripSummaryItems.sortedBy { it.id.get() })
            }
            .catch { exception ->
                Timber.e(exception)
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    fun setActionButtonHandlerFactory(actionButtonHandlerFactory: ActionButtonHandlerFactory?) {
        actionButtonHandler = actionButtonHandlerFactory?.createHandler(this)
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

    private fun setupButtons(tripGroup: TripGroup) {
        if (tripGroup.displayTrip == null) return
        viewModelScope.launch {
            actionButtonHandler?.let { handler ->
                val actions =
                    handler.getActions(context, tripGroup.displayTrip!!).distinctBy { it.text }
                if (buttons.size != actions.size) {
                    buttons.clear()
                    actions.forEach {
                        buttons.add(ActionButtonViewModel(context, it))
                    }
                } else {
                    actions.forEachIndexed { i, button ->
                        buttons[i].update(context, button)
                    }
                }
            }
        }
    }

    private fun setTitleAndSubtitle(tripGroup: TripGroup, tripId: Long) {
        val trip = tripGroup.trips?.firstOrNull { it.id == tripId } ?: tripGroup.displayTrip
        if (trip == null || trip.from == null || trip.to == null) return
        if (trip.isDepartureTimeFixed) {
            durationTitle.set("${printTime.print(trip.startDateTime)} - ${printTime.print(trip.endDateTime)}")
            arriveAtTitle.set(formatDuration(context, trip.startTimeInSecs, trip.endTimeInSecs))
        } else {
            durationTitle.set(formatDuration(context, trip.startTimeInSecs, trip.endTimeInSecs))
            if (!trip.queryIsLeaveAfter()) {
                arriveAtTitle.set(
                    context.resources.getString(
                        R.string.departs__pattern,
                        printTime.print(trip.startDateTime)
                    ).capitalize()
                )
            } else {
                arriveAtTitle.set(
                    context.resources.getString(
                        R.string.arrives__pattern,
                        printTime.print(trip.endDateTime)
                    ).capitalize()
                )
            }
        }

        isHideExactTimes.set(trip.isHideExactTimes || trip.segments.any { it.isHideExactTimes })
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
            if (segment?.id == tripSegment.id) {
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
        val time = when (tripSegment.type) {
            SegmentType.DEPARTURE -> printTime.print(tripSegment.startDateTime)
            SegmentType.ARRIVAL -> printTime.print(tripSegment.endDateTime)
            else -> null
        }

        var topConnectionColor: Int = Color.TRANSPARENT
        var bottomConnectionColor: Int = Color.TRANSPARENT

        val connectionColor = if (tripSegment.type == SegmentType.DEPARTURE) {
            nextSegment?.lineColor() ?: Color.TRANSPARENT
        } else {
            previousSegment?.lineColor() ?: Color.TRANSPARENT
        }

        if (tripSegment.type == SegmentType.DEPARTURE) {
            bottomConnectionColor = connectionColor
        }
        if (tripSegment.type == SegmentType.ARRIVAL) {
            topConnectionColor = connectionColor
        }
        viewModel.setupSegment(
            viewType = TripSegmentItemViewModel.SegmentViewType.TERMINAL,
            title = processedText(tripSegment, tripSegment.action),
            startTime = time,
            lineColor = connectionColor,
            topConnectionColor = topConnectionColor,
            bottomConnectionColor = bottomConnectionColor
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
        if (tripSegment.singleLocation != null && !tripSegment.singleLocation.address.isNullOrEmpty()) {
            location = tripSegment.singleLocation.displayAddress
                ?: tripSegment.singleLocation.address
        }
        if (!tripSegment.sharedVehicle?.garage()?.address.isNullOrEmpty()) {
            location = tripSegment.sharedVehicle.garage()?.address!!
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
            startTime = printTime.print(tripSegment.endDateTime),
            endTime = endTime,
            delay = delay,
            hasRealtime = (realtimeTripSegment != null),
            topConnectionColor = tripSegment.lineColor(),
            bottomConnectionColor = nextSegment?.lineColor() ?: Color.TRANSPARENT,
            isStationaryItem = true
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
            lineColor = tripSegment.lineColor()
        )
    }

    private fun setTripGroup(tripGroup: TripGroup, tripId: Long, savedInstanceState: Bundle?) {
        tripGroupRelay.accept(tripGroup)
        val newItems = ArrayList<Any>()
        val trip = tripGroup.trips?.firstOrNull { it.id == tripId } ?: tripGroup.displayTrip
        if (trip != null) {
            this.trip = trip
            val tripSegments = trip.segments
            segmentViewModels.clear()

            _mapTiles.postValue(tripSegments.firstOrNull { it.mapTiles != null }?.mapTiles)

            generateSummaryItems(
                tripSegments.filter { it.visibility == Visibilities.VISIBILITY_IN_SUMMARY }
            )

            tripSegments.forEachIndexed { index, segment ->
                val previousSegment = tripSegments.elementAtOrNull(index - 1)
                val nextSegment = tripSegments.elementAtOrNull(index + 1)

                val viewModel = segmentViewModelProvider.get()
                viewModel.alertsClicked.subscribe {
                    alertsClicked.accept(it)
                }.autoClear()

                viewModel.externalActionClicked.subscribe {
                    externalActionClicked.accept(it)
                }.autoClear()

                viewModel.onClick.observable.subscribe {
                    it.tripSegment?.let { segment ->
                        segmentClicked.accept(segment)
                    }
                }.autoClear()
                viewModel.tripSegment = segment

                if (segment.type == SegmentType.ARRIVAL || segment.type == SegmentType.DEPARTURE) {
                    addTerminalItem(viewModel, segment, previousSegment, nextSegment)
                } else if (segment.isStationary && ((segment.type == null && segment.startStopCode == null) || segment.type == SegmentType.STATIONARY)) {
                    addStationaryItem(viewModel, segment, previousSegment, nextSegment)
                } else {
                    if (nextSegment != null && !nextSegment.isStationary && nextSegment.type != SegmentType.ARRIVAL) {
                        val bridgeModel = segmentViewModelProvider.get()
                        bridgeModel.tripSegment = segment
                        addMovingItem(bridgeModel, segment)
                        bridgeModel.onClick.observable.subscribe {
                            it.tripSegment?.let { segment ->
                                segmentClicked.accept(segment)
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
                if (tripSegmentSummaryItem.none { it.id.get() == segment.id }) {
                    tripSegmentSummaryItem.add(
                        segment.generateTripPreviewHeader(context, drawable, printTime).getSummaryItem()
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

    private fun setupGetOffAlert(tripGroup: TripGroup, trip: Trip): TripSegmentGetOffAlertsViewModel? {
        try {
            val isOn = GetOffAlertCache.isTripAlertStateOn(trip.tripUuid)

            val getOffAlertsViewModel = TripSegmentGetOffAlertsViewModel(trip, isOn, tripUpdater, remindersRepository)

            getOffAlertsViewModel.alertStateListener = {
                setupButtons(tripGroup)
                _updatedState.postValue(Unit)
            }
            val messageTypes =
                trip.segments.flatMap { it.geofences.orEmpty() }.map { it.messageType }

            val startSegmentStartTimeInSecs =
                trip.segments?.minByOrNull { it.startTimeInSecs }?.startTimeInSecs ?: 0

            val reminderInMinutes =
                runBlocking { remindersRepository.getTripNotificationReminderMinutes() }

            val reminder = TimeUnit.MINUTES.toSeconds(reminderInMinutes)

            val currentDateTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(
                DateTime(System.currentTimeMillis(), trip.from.dateTimeZone).millis
            )

            val isAboutToStart = if(startSegmentStartTimeInSecs > currentDateTimeInSeconds) {
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
                    ),TripSegmentGetOffAlertDetailViewModel(
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
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
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

    override fun onItemClick(tag: String, viewModel: ActionButtonViewModel) {
        if(tag == ActionButtonHandler.ACTION_TAG_ALERT) {
            tripSegmentGetOffAlertsViewModel?.apply {
                setAlertState(getOffAlertStateOn.value?.not() ?: false)
            }
        } else {
            actionButtonHandler?.actionClicked(
                context, tag, this.trip ?: tripGroup.displayTrip!!, viewModel
            )
        }
    }

}
