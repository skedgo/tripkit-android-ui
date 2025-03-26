package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skedgo.TripKit
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import org.joda.time.DateTime
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class TripResultTripViewModel : ViewModel() {
    var trip: Trip? = null
        set(value) {
            field = value
            checkQuickBooking() // Recalculate visibility whenever trip is updated
        }
    val title = MutableLiveData<String>()
    val subtitle = MutableLiveData<String>()
    val isMissedPreBooking = MutableLiveData<Boolean>()
    val isHideExactTimes = MutableLiveData<Boolean>()
    val contentDescription = MutableLiveData<String>()
    var clickFlow: MutableSharedFlow<Trip>? = null
    var quickBookingActionClickFlow: MutableSharedFlow<TripSegment>? = null
    val segments = ArrayList<TripSegmentViewModel>()
    val weightedScore = MutableLiveData<String>()
    val isCancelled = MutableLiveData(false)

    private val _hasQuickBooking = MutableLiveData<Boolean>(false)
    val hasQuickBooking: LiveData<Boolean> = _hasQuickBooking

    fun onItemClicked() {
        viewModelScope.launch {
            trip?.let {
                clickFlow?.emit(it)
            }
        }
    }

    fun onQuickBookingActionClicked() {
        viewModelScope.launch {
            trip?.quickBookingSegment?.let { quickBookingActionClickFlow?.emit(it) }
        }
    }

    private fun checkQuickBooking() {
        _hasQuickBooking.value = trip?.quickBookingSegment != null
    }
}

class TripResultViewModel @Inject constructor(
    private val context: Context,
    private val tripSegmentHelper: TripSegmentHelper,
    private val printTime: PrintTime,
    private val resources: Resources,
    private val transportModeSharedPreference: TransportModeSharedPreference
) : RxViewModel() {
    val onItemClicked: TapAction<TripResultViewModel> = TapAction.create { this }
    val onMoreButtonClicked: TapAction<TripResultViewModel> = TapAction.create { this }
    var clickFlow: MutableSharedFlow<Trip>? = null
    var quickBookingActionClickFlow: MutableSharedFlow<TripSegment>? = null

    lateinit var group: TripGroup
    lateinit var trip: Trip
    private var alternateTrip: Trip? = null

    // Badge
    private val _badgeDrawable = MutableLiveData<Drawable?>()
    val badgeDrawable: LiveData<Drawable?> = _badgeDrawable

    private val _badgeText = MutableLiveData<String?>()
    val badgeText: LiveData<String?> = _badgeText

    private val _badgeTextColor = MutableLiveData<Int?>()
    val badgeTextColor: LiveData<Int?> = _badgeTextColor

    private val _badgeVisible = MutableLiveData<Boolean>(false)
    val badgeVisible: LiveData<Boolean> = _badgeVisible

    val tripResults = MutableLiveData<MutableList<TripResultTripViewModel>>()
    val showMoreTrips = MutableLiveData(false)
    val hasTripLabels = MutableLiveData(true)

    var actionButtonHandler: ActionButtonHandler? = null

    // Footer
    val alternateTripVisible = MutableLiveData(false)
    val costVisible = MutableLiveData(true)
    val cost = MutableLiveData<String>()
    val moreButtonVisible = MutableLiveData(false)
    var moreButtonText = MutableLiveData<String>()
    var accessibilityLabel = MutableLiveData<String>()
    var otherTripGroups: List<Trip>? = null
    var classification = TripGroupClassifier.Classification.NONE

    private val _moneyCost = MutableLiveData<String>()
    val moneyCost: LiveData<String> = _moneyCost

    private val _isMoneyCostVisible = MutableLiveData<Boolean>()
    val isMoneyCostVisible: LiveData<Boolean> = _isMoneyCostVisible

    private val _availabilityInfo = MutableLiveData<String>()
    val availabilityInfo: LiveData<String> = _availabilityInfo

    private val _isActionEnabled = MutableLiveData<Boolean>()
    val isActionEnabled: LiveData<Boolean> = _isActionEnabled

    private val globalConfigs by lazy {
        TripKit.getInstance().configs()
    }

    fun toggleShowMore() {
        showMoreTrips.value = !(showMoreTrips.value ?: false)

        if (showMoreTrips.value == true) {
            otherTripGroups?.forEach {
                addTripToList(it)
            }
            moreButtonText.value = context.resources.getString(R.string.less)
            moreButtonVisible.value = true
        } else {
            otherTripGroups?.let { otherTrips ->
                removeFromTripList(otherTrips.map { tripToTripResultTripViewModel(it) })
            }
            moreButtonText.value = context.resources.getString(R.string.more)
            moreButtonVisible.value = true
        }

        sortTripResults()
    }

    private fun sortTripResults() {
        tripResults.value = tripResults.value?.sortedBy {
            it.trip?.startTimeInSecs ?: Long.MAX_VALUE
        }?.toMutableList()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null
            || other !is TripResultViewModel
            || other.group.uuid() != group.uuid()
            || other.trip.segmentList.size != trip.segmentList.size)
            return false
        trip.segmentList.forEachIndexed { index, value ->
            if (value.bookingHashCode != other.trip.segmentList[index].bookingHashCode) return false
        }

        if (moreButtonText.value != other.moreButtonText.value) {
            return false
        }
        return true
    }

    fun setTripGroup(
        context: Context,
        tripgroup: TripGroup,
        classification: TripGroupClassifier.Classification?
    ) {
        tripResults.value = mutableListOf()
        group = tripgroup
        trip = tripgroup.displayTrip!!
        otherTripGroups = tripgroup.trips?.filterNot { it.uuid == trip.uuid }
        alternateTrip = otherTripGroups?.firstOrNull()

        if (classification != null && classification != TripGroupClassifier.Classification.NONE) {
            setBadge(classification)
        } else {
            _badgeDrawable.value = null
            _badgeText.value = null
            _badgeTextColor.value = null
            _badgeVisible.value = false
        }

        addTripToList(trip)

        alternateTrip?.let {
            addTripToList(it)
            val otherTrips = otherTripGroups?.toMutableList()
            otherTrips?.removeAll { otherTrip -> otherTrip.tripId == it.tripId }
            otherTripGroups = otherTrips
        }

        setCost()
        setMoneyCost()

        if (otherTripGroups.isNullOrEmpty()) {
            if(!trip.hasQuickBooking() && trip.segmentList.none { it.bookingHasConfirmation } ) {
                val actionButtonText =
                    actionButtonHandler?.getPrimaryAction(context, trip)

                actionButtonText?.let {
                    moreButtonText = it
                    moreButtonVisible.value = true
                }
            }
        } else {
            moreButtonText.value = context.resources.getString(R.string.more)
            moreButtonVisible.value = true
        }

        trip.availabilityInfo?.let { _availabilityInfo.postValue(it) }
        _isActionEnabled.postValue(trip.getAvailability() == Availability.Available)
    }

    private fun addTripToList(trip: Trip) {
        val results = tripResults.value.orEmpty().toMutableList()
        results.add(tripToTripResultTripViewModel(trip))
        tripResults.value = results
    }

    private fun removeFromTripList(trips: List<TripResultTripViewModel>) {
        val results = tripResults.value.orEmpty().toMutableList()
        results.removeAll { trips.any { toRemove -> it.trip == toRemove.trip } }
        tripResults.value = results
    }

    private fun tripToTripResultTripViewModel(trip: Trip): TripResultTripViewModel {
        val newVm = TripResultTripViewModel()
        newVm.trip = trip
        newVm.isCancelled.value = trip.getAvailability() == Availability.Cancelled
        newVm.clickFlow = clickFlow
        newVm.quickBookingActionClickFlow = quickBookingActionClickFlow
        newVm.title.value = buildTitle(context, trip)
        newVm.weightedScore.value = trip.weightedScore.toString()
        newVm.subtitle.value = buildSubtitle(context, trip)
        newVm.contentDescription.value = buildContentDescription(trip)
        newVm.isMissedPreBooking.value =
            trip.segmentList?.first()?.availability.equals(Availability.MissedPrebookingWindow.value)
        newVm.isHideExactTimes.value =
            trip.hideExactTimes || trip.segmentList.any { it.isHideExactTimes }
        accessibilityLabel.value =
            getAccessibilityLabel() ?: context.getString(R.string.book)
        setSegments(newVm.segments, trip)

        return newVm
    }

    private fun getAccessibilityLabel(): String? {
        var mAccessibilityLabel: String? = null
        trip.segmentList?.forEach {
            if (!it.booking?.accessibilityLabel.isNullOrEmpty()) {
                mAccessibilityLabel = it.booking?.accessibilityLabel
            }
        }
        return mAccessibilityLabel
    }

    private fun buildContentDescription(trip: Trip): String? {
        val contentDescBuilder = StringBuilder()
        trip.segmentList.forEach {
            if (it.modeInfo != null) {
                contentDescBuilder.append(it.modeInfo?.alternativeText).append(" ")
                contentDescBuilder.append(it.modeInfo?.description).append(" ")
                contentDescBuilder.append("for ").append(
                    buildTitle(context, trip).replace(
                        context.getString(R.string.str_mins),
                        context.getString(R.string.str_minutes)
                    )
                ).append(". ")
            }
            if (it == trip.segmentList.last()) {
                contentDescBuilder.append(buildSubtitle(context, trip))
            }
        }
        return contentDescBuilder.toString()
    }

    private fun setBadge(classification: TripGroupClassifier.Classification) {
        this.classification = classification
        val (drawableRes, textRes, textColor) = when (classification) {
            TripGroupClassifier.Classification.EASIEST -> {
                Triple(R.drawable.ic_like_circle, R.string.easiest, R.color.classification_easiest)
            }
            TripGroupClassifier.Classification.CHEAPEST -> {
                Triple(
                    R.drawable.ic_money_sign_circle,
                    R.string.cheapest,
                    R.color.classification_cheapest
                )
            }
            TripGroupClassifier.Classification.FASTEST -> {
                Triple(
                    R.drawable.ic_lightning_circle,
                    R.string.fastest,
                    R.color.classification_fastest
                )
            }
            TripGroupClassifier.Classification.GREENEST -> {
                Triple(
                    R.drawable.ic_leaf_circle,
                    R.string.greenest,
                    R.color.classification_greenest
                )
            }
            TripGroupClassifier.Classification.HEALTHIEST -> {
                Triple(
                    R.drawable.ic_heart_circle,
                    R.string.healthiest,
                    R.color.classification_healthiest
                )
            }
            TripGroupClassifier.Classification.RECOMMENDED -> {
                Triple(
                    R.drawable.ic_recommeneded_circle,
                    R.string.recommended,
                    R.color.classification_recommended
                )
            }
            else -> {
                Triple(-1, -1, -1)
            }
        }
        if (drawableRes != -1) {
            // badgeDrawable.set(ContextCompat.getDrawable(context, drawableRes))
            _badgeDrawable.value = ContextCompat.getDrawable(context, drawableRes)
            // badgeText.set(context.getString(textRes))
            _badgeText.value = context.getString(textRes)
            // badgeTextColor.set(ContextCompat.getColor(context, textColor))
            _badgeTextColor.value = ContextCompat.getColor(context, textColor)
            //badgeVisible.set(true)
            _badgeVisible.value = true
        }
    }


    private fun buildTitle(context: Context, _trip: Trip): String {
        return if (_trip.isDepartureTimeFixed()) {
            showTimeRange(_trip.startDateTime, _trip.endDateTime)
        } else {
            formatDuration(context, _trip.startTimeInSecs, _trip.endTimeInSecs)
        }
    }

    /**
     * For example, '09:40am - 10:40am (59mins)'
     */
    private fun showTimeRange(startDateTime: DateTime, endDateTime: DateTime): String {
        return "${printTime.printLocalTime(startDateTime.toLocalTime())} - ${
            printTime.printLocalTime(
                endDateTime.toLocalTime()
            )
        }"
    }

    /**
     * For example, 1hr 50mins
     */
    private fun formatDuration(
        context: Context,
        startTimeInSecs: Long,
        endTimeInSecs: Long
    ): String =
        TimeUtils.getDurationInDaysHoursMins(context, (endTimeInSecs - startTimeInSecs).toInt())

    private fun buildSubtitle(context: Context, _trip: Trip): String {
        return if (_trip.isDepartureTimeFixed()) {
            formatDuration(context, _trip.startTimeInSecs, _trip.endTimeInSecs)
        } else if (!_trip.queryIsLeaveAfter) {
            resources.getString(
                R.string.departs__pattern,
                printTime.printLocalTime(_trip.startDateTime.toLocalTime())
            )
        } else {
            resources.getString(
                R.string.arrives__pattern,
                printTime.printLocalTime(_trip.endDateTime.toLocalTime())
            )
        }
    }

    private fun setSegments(_segments: ArrayList<TripSegmentViewModel>, _trip: Trip) {
        _segments.clear()
        _trip.getSummarySegments().forEach { segment ->
            val newModel = TripSegmentViewModel(context, printTime, transportModeSharedPreference)
            newModel.setSegment(_trip, segment)
            _segments.add(newModel)
        }
    }


    private fun setCost() {
        var displayCost = trip.getDisplayCost(resources.getString(R.string.free))
        var displayCarbon = trip.getDisplayCarbonCost()
        var displayCalories = trip.getDisplayCalories()
        var builder = StringBuilder()

        displayCost?.let {
            builder.append(it)
            builder.append(" · ")
        }

        builder.append(displayCalories)
        builder.append(" · ")

        if (displayCarbon != null) {
            builder.append(displayCarbon)
            builder.append(" CO₂");
        } else {
            builder.append(resources.getString(R.string.no_co_2));
        }

        cost.value = builder.toString()

        costVisible.value = !globalConfigs.hideTripMetrics()
        hasTripLabels.value = globalConfigs.hasTripLabels()
    }

    private fun setMoneyCost() {
        if (trip.moneyCost == 0f && trip.moneyUsdCost == 0f) {
            _isMoneyCostVisible.postValue(false)
            return
        }

        trip.getDisplayCostUsd()?.let {
            _isMoneyCostVisible.postValue(globalConfigs.hideTripMetrics())
            _moneyCost.postValue(it)
        }
    }
}