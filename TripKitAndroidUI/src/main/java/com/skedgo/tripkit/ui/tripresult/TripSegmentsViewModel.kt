package com.skedgo.tripkit.ui.tripresult


import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.creditsources.CreditSourcesOfDataViewModel
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonContainer
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.skedgo.tripkit.ui.utils.TripSegmentActionProcessor
import com.squareup.otto.Bus
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import timber.log.Timber
import java.util.*
import java.util.Collections.emptyList
import javax.inject.Inject
import javax.inject.Provider


class TripSegmentsViewModel @Inject internal constructor(
        private val context: Context,
        private val printTime: PrintTime,
        private val segmentViewModelProvider: Provider<TripSegmentItemViewModel>,
        private val creditSourcesOfDataViewModelProvider: Provider<CreditSourcesOfDataViewModel>,
        private val updateTripForRealtime: UpdateTripForRealtime,
        private val tripGroupRepository: TripGroupRepository,
        private val tripSegmentActionProcessor: TripSegmentActionProcessor,
        private val getAlternativeTripForAlternativeService: GetAlternativeTripForAlternativeService) : RxViewModel(), ActionButtonContainer, ActionButtonClickListener {

  val segmentViewModels: MutableList<TripSegmentItemViewModel> = ArrayList()
  val buttons = ObservableArrayList<ActionButtonViewModel>()
  val buttonsBinding = ItemBinding.of<ActionButtonViewModel>(BR.viewModel, R.layout.trip_segment_action_button)
          .bindExtra(BR.listener, this)
  val itemViewModels = ObservableField(emptyList<Any>())
  val itemBinding = ItemBinding.of(
          OnItemBindClass<Any>()
                  .map(CreditSourcesOfDataViewModel::class.java, BR.viewModel, R.layout.credit_sources_of_data)
                  .map(TripSegmentItemViewModel::class.java, BR.viewModel, R.layout.trip_segment)
  )
  val showCloseButton = ObservableBoolean(false)

  internal val onStreetViewTapped = PublishSubject.create<Location>()
  private val creditSourcesOfDataViewModel = BehaviorRelay.create<CreditSourcesOfDataViewModel>()
  private val tripGroupRelay = BehaviorRelay.create<TripGroup>()

  internal var itemsChangeEmitter = PublishSubject.create<List<TripSegmentItemViewModel>>()
  internal var bookingForm: BookingForm? = null
  private var internalBus: Bus? = null
  val alertsClicked = BehaviorRelay.create<ArrayList<RealtimeAlert>>()
  val segmentClicked = BehaviorRelay.create<TripSegment>()
  var durationTitle = ObservableField<String>()
  var arriveAtTitle = ObservableField<String>()
  private var actionButtonHandler: ActionButtonHandler? = null

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

  init {
  }

  fun setActionButtonHandlerFactory(actionButtonHandlerFactory: ActionButtonHandlerFactory?) {
    actionButtonHandler = actionButtonHandlerFactory?.createHandler(this)
  }
  fun setInternalBus(bus: Bus) {
    this.internalBus = bus
  }

  fun loadTripGroup(tripGroupId: String, tripId: Long, savedInstanceState: Bundle?) {
    tripGroupRepository.getTripGroup(tripGroupId)
            .observeOn(mainThread())
            .onErrorResumeNext (Observable.empty())
        .subscribe { tripGroup ->
          setTitleAndSubtitle(tripGroup, tripId)
          setTripGroup(tripGroup, tripId, savedInstanceState)
          setupButtons(tripGroup)
        }
            .autoClear()
  }

  private fun setupButtons(tripGroup: TripGroup) {
    if (tripGroup.displayTrip == null) return

    actionButtonHandler?.let { handler ->
      val actions = handler.getActions(context, tripGroup.displayTrip!!)
      if (buttons.size != actions.size) {
        buttons.clear()
        actions.forEach {
          buttons.add(ActionButtonViewModel(context, it))
        }
      } else {
        actions.forEachIndexed {i, button ->
          buttons[i].update(context, button)
        }
      }
    }
  }
  private fun setTitleAndSubtitle(tripGroup: TripGroup, tripId: Long) {
    val trip = tripGroup.trips?.firstOrNull { it.id == tripId } ?: tripGroup.displayTrip
    if (trip == null || trip.from == null || trip.to == null) return
    if (trip.isDepartureTimeFixed) {
      durationTitle.set("${printTime.print(trip.startDateTime)} - ${printTime.print(trip.endDateTime)}")
      arriveAtTitle.set(formatDuration(trip.startTimeInSecs, trip.endTimeInSecs))
    } else {
      durationTitle.set(formatDuration(trip.startTimeInSecs, trip.endTimeInSecs))
      if (!trip.queryIsLeaveAfter()){
        arriveAtTitle.set(context.resources.getString(R.string.departs__pattern, printTime.print(trip.startDateTime)).capitalize())
      } else {
        arriveAtTitle.set(context.resources.getString(R.string.arrives__pattern, printTime.print(trip.endDateTime)).capitalize())
      }
    }


  }

  // TODO This function is duplicated in TripResultViewModel
  /**
   * For example, 1hr 50mins
   */
  private fun formatDuration(startTimeInSecs: Long, endTimeInSecs: Long): String  = TimeUtils.getDurationInDaysHoursMins((endTimeInSecs - startTimeInSecs).toInt())

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

//  fun onAlternativeTimeForTripSegmentSelected(tripSegmentId: Long, selectedService: TimetableEntry): Completable {
//    return tripGroupRelay.hide()
//        .firstOrError()
//        .flatMapCompletable { tripGroup ->
//          getAlternativeTripForAlternativeService
//              .execute(tripGroup.displayTrip!!, tripSegmentId, selectedService)
//              .map { it.displayTrip!! }
//              .flatMapCompletable { newTrip -> tripGroupRepository.addTripToTripGroup(tripGroup.uuid(), newTrip) }
//        }
//  }

  private fun processedText(segment: TripSegment, text: String?): String {
    return if (!text.isNullOrBlank()) {
      tripSegmentActionProcessor.processText(context, segment, text, false)
    } else {
      String()
    }
  }

  private fun addTerminalItem(viewModel: TripSegmentItemViewModel,
                              tripSegment: TripSegment,
                                previousSegment: TripSegment? = null,
                                nextSegment: TripSegment? = null) {

    val time = when(tripSegment.type) {
        SegmentType.DEPARTURE -> printTime.print(tripSegment.startDateTime)
        SegmentType.ARRIVAL -> printTime.print(tripSegment.endDateTime)
        else -> null
    }

    var topConnectionColor : Int = Color.TRANSPARENT
    var bottomConnectionColor : Int = Color.TRANSPARENT

    var connectionColor = if (tripSegment.type == SegmentType.DEPARTURE) {
      nextSegment?.lineColor() ?: Color.TRANSPARENT
    } else {
      previousSegment?.lineColor() ?: Color.TRANSPARENT
    }

    if (tripSegment.type == SegmentType.DEPARTURE) { bottomConnectionColor = connectionColor }
    if (tripSegment.type == SegmentType.ARRIVAL) { topConnectionColor = connectionColor }

    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.TERMINAL,
                            title = processedText(tripSegment, tripSegment.action),
                            startTime = time,
                            lineColor = connectionColor,
                            topConnectionColor = topConnectionColor,
                            bottomConnectionColor = bottomConnectionColor)
  }
  private fun addStationaryItem(viewModel: TripSegmentItemViewModel,
                                tripSegment: TripSegment,
                                previousSegment: TripSegment? = null,
                                nextSegment: TripSegment? = null) {
    var startTime : String? = when {
      nextSegment != null -> printTime.print(nextSegment.startDateTime)
      else -> null
    }

    var endTime : String? = null
    var delay = 0L

    nextSegment?.let {
      if (it.isRealTime) {
        delay = it.startTimeInSecs - it.timetableStartTime
        if (delay != 0L) {
          startTime = printTime.print(it.timetableStartDateTime)
        }

        endTime = when {
          delay == 0L -> null
          else -> printTime.print(it.timetableStartDateTime)
        }
      }
    }

    val possibleDescription = when {
      !nextSegment?.platform.isNullOrBlank() -> context.getString(R.string.platform, nextSegment!!.platform)
      else -> null
    }

    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.STATIONARY,
            title = tripSegment.singleLocation?.displayName ?: context.resources.getString(R.string.location),
            description = possibleDescription,
            startTime = startTime,
            endTime = endTime,
            delay = delay,
            hasRealtime = nextSegment?.isRealTime ?: false,
            topConnectionColor = previousSegment?.lineColor() ?: Color.TRANSPARENT,
            bottomConnectionColor = nextSegment?.lineColor() ?: Color.TRANSPARENT)
  }

  private fun addStationaryBridgeItem(viewModel: TripSegmentItemViewModel,
                                tripSegment: TripSegment,
                                nextSegment: TripSegment? = null) {

    val possibleTitle = nextSegment?.from?.displayName ?: tripSegment.to?.displayName
    val possibleDescription = when {
      !nextSegment?.platform.isNullOrBlank() -> context.getString(R.string.platform, nextSegment!!.platform)
      else -> null
    }
    var endTime = if (nextSegment != null) printTime.print(nextSegment.startDateTime) else null

    // If it's a real-time service, we show the time in green if it's on-time, yellow if it's early, and red if it's late.
    // In both cases, we show the original time crossed out below the current real-time information.
    var delay = 0L
    var realtimeTripSegment = when {
      tripSegment.isRealTime -> tripSegment
      (nextSegment != null) && nextSegment.isRealTime -> nextSegment
      else -> null
    }

    realtimeTripSegment?.let {
      if (it.from != null) {
        delay = it.startTimeInSecs - it.timetableStartTime
        endTime = when {
          delay == 0L -> null
          else -> printTime.print(it.timetableStartDateTime) }
      } else if (it.to != null) {
        delay = it.endTimeInSecs - it.timetableEndTime
        endTime = when {
          delay == 0L -> null
          else -> printTime.print(it.timetableEndDateTime) }
      }
    }

    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.STATIONARY_BRIDGE,
            title = possibleTitle ?: context.resources.getString(R.string.location),
            description = possibleDescription,
            startTime = printTime.print(tripSegment.endDateTime),
            endTime = endTime,
            delay = delay,
            hasRealtime = (realtimeTripSegment != null),
            topConnectionColor = tripSegment.lineColor(),
            bottomConnectionColor = nextSegment?.lineColor() ?: Color.TRANSPARENT)
  }

  private fun addMovingItem(viewModel: TripSegmentItemViewModel,
                                      tripSegment: TripSegment) {
    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.MOVING,
            title = processedText(tripSegment, tripSegment.action),
            description = tripSegment.getDisplayNotes(context.resources, false),
            lineColor = tripSegment.lineColor())
  }
  private fun setTripGroup(tripGroup: TripGroup, tripId: Long, savedInstanceState: Bundle?) {
    tripGroupRelay.accept(tripGroup)
    val newItems = ArrayList<Any>()
    val trip = tripGroup.trips?.firstOrNull { it.id == tripId } ?: tripGroup.displayTrip
    if (trip != null) {
      val tripSegments = tripGroup.displayTrip!!.segments
      segmentViewModels.clear()
      tripSegments.forEachIndexed { index, segment ->
        var previousSegment = tripSegments.elementAtOrNull(index - 1)
        val nextSegment = tripSegments.elementAtOrNull(index + 1)

        var viewModel = segmentViewModelProvider.get()
        viewModel.alertsClicked.subscribe {
          alertsClicked.accept(it)
        }.autoClear()
        viewModel.onClick.observable.subscribe {
          it.tripSegment?.let {
            segmentClicked.accept(it)
          }
        }.autoClear()
        viewModel.tripSegment = segment

        if (segment.type == SegmentType.ARRIVAL || segment.type == SegmentType.DEPARTURE) {
          addTerminalItem(viewModel, segment, previousSegment, nextSegment)
        } else if (segment.type == SegmentType.STATIONARY) {
          addStationaryItem(viewModel, segment, previousSegment, nextSegment)
        } else {
          if (nextSegment != null && !nextSegment.isStationary) {
            var bridgeModel = segmentViewModelProvider.get()
            bridgeModel.tripSegment = segment
            addMovingItem(bridgeModel, segment)
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

    if (tripGroup.sources != null && tripGroup.sources!!.size > 0) {
      val creditSourcesOfDataViewModel = creditSourcesOfDataViewModelProvider.get()
      creditSourcesOfDataViewModel.changeSources(tripGroup.sources!!)
      newItems.add(creditSourcesOfDataViewModel)
      this.creditSourcesOfDataViewModel.accept(creditSourcesOfDataViewModel)
    }
    itemViewModels.set(newItems)
  }

  private fun startUpdate() {
    updateTripForRealtime.start(tripGroupRelay.hide())
  }

  override fun scope(): CoroutineScope = viewModelScope
  override fun replaceTripGroup(tripGroupUuid: String, newTripGroup: TripGroup) {}
  override fun onItemClick(tag: String, viewModel: ActionButtonViewModel) {
    actionButtonHandler?.actionClicked(context, tag, tripGroup.displayTrip!!, viewModel)
  }

}
