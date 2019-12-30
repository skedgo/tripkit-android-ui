package com.skedgo.tripkit.ui.tripresult


import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.creditsources.CreditSourcesOfDataViewModel
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.utils.TripSegmentActionProcessor
import com.squareup.otto.Bus
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.subjects.PublishSubject
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
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
        private val getAlternativeTripForAlternativeService: GetAlternativeTripForAlternativeService) : RxViewModel() {

  val segmentViewModels: MutableList<TripSegmentItemViewModel> = ArrayList()
  val itemViewModels = ObservableField(emptyList<Any>())
  val itemBinding = ItemBinding.of(
          OnItemBindClass<Any>()
                  .map(CreditSourcesOfDataViewModel::class.java, BR.viewModel, R.layout.credit_sources_of_data)
                  .map(TripSegmentItemViewModel::class.java, BR.viewModel, R.layout.trip_segment)
  )

  internal val onStreetViewTapped = PublishSubject.create<Location>()
  private val creditSourcesOfDataViewModel = BehaviorRelay.create<CreditSourcesOfDataViewModel>()
  private val tripGroupRelay = BehaviorRelay.create<TripGroup>()

  internal var itemsChangeEmitter = PublishSubject.create<List<TripSegmentItemViewModel>>()
  internal var bookingForm: BookingForm? = null
  private var internalBus: Bus? = null

  var durationTitle = ObservableField<String>()
  var arriveAtTitle = ObservableField<String>()


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
    internal set(tripGroup) = setTripGroup(tripGroup, null)

  init {
  }

  fun setInternalBus(bus: Bus) {
    this.internalBus = bus
  }

  fun loadTripGroup(tripGroupId: String, savedInstanceState: Bundle?) {
    tripGroupRepository.getTripGroup(tripGroupId)
            .observeOn(mainThread())
            .onErrorResumeNext (Observable.empty())
        .subscribe { tripGroup ->
          setTitleAndSubtitle(tripGroup)
          setTripGroup(tripGroup, savedInstanceState) }
            .autoClear()
  }

  private fun setTitleAndSubtitle(tripGroup: TripGroup) {
    val trip = tripGroup.displayTrip
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

    var connectionColor = if (tripSegment.type == SegmentType.DEPARTURE) {
      nextSegment?.lineColor() ?: Color.TRANSPARENT
    } else {
      previousSegment?.lineColor() ?: Color.TRANSPARENT
    }

    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.TERMINAL,
                            title = processedText(tripSegment, tripSegment.action),
                            startTime = time,
                            lineColor = connectionColor)
  }
  private fun addStationaryItem(viewModel: TripSegmentItemViewModel,
                                tripSegment: TripSegment,
                                previousSegment: TripSegment? = null,
                                nextSegment: TripSegment? = null) {
    val startTime = if (tripSegment.from != null) printTime.print(tripSegment.startDateTime) else null
    val endTime = if (tripSegment.to != null) printTime.print(tripSegment.endDateTime) else null
    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.STATIONARY,
            title = tripSegment.singleLocation?.displayName ?: context.resources.getString(R.string.location),
            description = processedText(tripSegment, tripSegment.action),
            startTime = startTime,
            endTime = endTime,
            topConnectionColor = previousSegment?.lineColor() ?: Color.TRANSPARENT,
            bottomConnectionColor = nextSegment?.lineColor() ?: Color.TRANSPARENT)
  }

  private fun addStationaryBridgeItem(viewModel: TripSegmentItemViewModel,
                                tripSegment: TripSegment,
                                nextSegment: TripSegment? = null) {
    val possibleTitle = nextSegment?.from?.displayName ?: tripSegment.to?.displayName
    val endTime = if (nextSegment != null) printTime.print(nextSegment.startDateTime) else null

    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.STATIONARY_BRIDGE,
            title = possibleTitle ?: context.resources.getString(R.string.location),
            startTime = printTime.print(tripSegment.endDateTime),
            endTime = endTime,
            topConnectionColor = tripSegment.lineColor(),
            bottomConnectionColor = nextSegment?.lineColor() ?: Color.TRANSPARENT)
  }

  private fun addMovingItem(viewModel: TripSegmentItemViewModel,
                                      tripSegment: TripSegment) {
    viewModel.setupSegment(viewType = TripSegmentItemViewModel.SegmentViewType.MOVING,
            title = processedText(tripSegment, tripSegment.action),
            description = tripSegment.getDisplayNotes(context.resources),
            lineColor = tripSegment.lineColor())
  }
  private fun setTripGroup(tripGroup: TripGroup, savedInstanceState: Bundle?) {
    tripGroupRelay.accept(tripGroup)
    val needsRestore = savedInstanceState != null
    val newItems = ArrayList<Any>()
    if (tripGroup.displayTrip != null) {
      val tripSegments = tripGroup.displayTrip!!.segments
      segmentViewModels.clear()
      tripSegments.forEachIndexed { index, segment ->
        Timber.d("Segment alerts is null or empty: %s", segment.alerts.isNullOrEmpty())
        var previousSegment = tripSegments.elementAtOrNull(index - 1)
        val nextSegment = tripSegments.elementAtOrNull(index + 1)

        var viewModel = segmentViewModelProvider.get()
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

}
