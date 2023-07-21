package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skedgo.TripKit
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.joda.time.DateTime
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TripResultTripViewModel : ViewModel() {
    var trip: Trip? = null
    val title = ObservableField<String>()
    val subtitle = ObservableField<String>()
    val isMissedPreBooking = ObservableField<Boolean>()
    val isHideExactTimes = ObservableField<Boolean>()
    val contentDescription = ObservableField<String>()
    var clickFlow: MutableSharedFlow<Trip>? = null
    val segments = ArrayList<TripSegmentViewModel>()
    fun onItemClicked() {
        viewModelScope.launch {
            trip?.let {
                clickFlow?.emit(it)
            }
        }
    }

}

class TripResultViewModel @Inject constructor(private val context: Context,
                                              private val tripSegmentHelper: TripSegmentHelper,
                                              private val printTime: PrintTime,
                                              private val resources: Resources)
    : RxViewModel() {
    val onItemClicked: TapAction<TripResultViewModel> = TapAction.create { this }
    val onMoreButtonClicked: TapAction<TripResultViewModel> = TapAction.create { this }
    var clickFlow: MutableSharedFlow<Trip>? = null

    lateinit var group: TripGroup
    lateinit var trip: Trip
    private var alternateTrip: Trip? = null

    // Badge
    val badgeDrawable = ObservableField<Drawable>()
    val badgeText = ObservableField<String>()
    val badgeTextColor = ObservableInt()
    val badgeVisible = ObservableBoolean(false)
    val tripResults = ObservableArrayList<TripResultTripViewModel>()
    val showMoreTrips = ObservableBoolean(false)
    val hasTripLabels = ObservableBoolean(true)

    var actionButtonHandler: ActionButtonHandler? = null

    // Footer
    val alternateTripVisible = ObservableBoolean(false)
    val costVisible = ObservableBoolean(true)
    val cost = ObservableField<String>()
    val moreButtonVisible = ObservableBoolean(false)
    var moreButtonText = ObservableField<String>()
    var accessibilityLabel = ObservableField<String>()
    var otherTripGroups: List<Trip>? = null
    var classification = TripGroupClassifier.Classification.NONE

    fun toggleShowMore() {
        showMoreTrips.set(!showMoreTrips.get())

        if (showMoreTrips.get()) {
            otherTripGroups?.forEach {
                addTripToList(it)
            }
            moreButtonText.set(context.resources.getString(R.string.less))
            moreButtonVisible.set(true)
        } else {
            otherTripGroups?.let { otherTrips ->
                removeFromTripList(otherTrips.map { tripToTripResultTripViewModel(it) })
            }
            moreButtonText.set(context.resources.getString(R.string.more))
            moreButtonVisible.set(true)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null
                || other !is TripResultViewModel
                || other.group.uuid() != group.uuid()
                || other.trip.segments.size != trip.segments.size)
            return false
        trip.segments.forEachIndexed { index, value ->
            if (value.bookingHashCode != other.trip.segments[index].bookingHashCode) return false
        }

        if (moreButtonText.get() != other.moreButtonText.get()) {
            return false
        }
        return true
    }

    fun setTripGroup(context: Context,
                     tripgroup: TripGroup,
                     classification: TripGroupClassifier.Classification?) {
        group = tripgroup
        trip = tripgroup.displayTrip!!
        otherTripGroups = tripgroup.trips?.filterNot { it.uuid() == trip.uuid() }
        alternateTrip = otherTripGroups?.firstOrNull()

        if (classification != null && classification != TripGroupClassifier.Classification.NONE) {
            setBadge(classification)
        }

        addTripToList(trip)

        alternateTrip?.let {
            addTripToList(it)
            val otherTrips = otherTripGroups?.toMutableList()
            otherTrips?.removeAll { otherTrip -> otherTrip.id == it.id }
            otherTripGroups = otherTrips
        }

        setCost()

        if (otherTripGroups.isNullOrEmpty()) {
            val actionButtonText = actionButtonHandler?.getPrimaryAction(context, trip)

            actionButtonText?.let {
                moreButtonText = actionButtonText
                moreButtonVisible.set(true)
            }
        } else {
            moreButtonText.set(context.resources.getString(R.string.more))
            moreButtonVisible.set(true)
        }
    }

    private fun addTripToList(trip: Trip) {
        tripResults.add(tripToTripResultTripViewModel(trip))
    }

    private fun removeFromTripList(trips: List<TripResultTripViewModel>) {
        tripResults.removeAll { trips.any { toRemove -> it.trip == toRemove.trip } }
    }

    private fun tripToTripResultTripViewModel(trip: Trip): TripResultTripViewModel {
        val newVm = TripResultTripViewModel()
        newVm.trip = trip
        newVm.clickFlow = clickFlow
        newVm.title.set(buildTitle(context, trip))
        newVm.subtitle.set(buildSubtitle(context, trip))
        newVm.contentDescription.set(buildContentDescription(trip))
        newVm.isMissedPreBooking.set(trip.segments?.first()?.availability.equals(Availability.MissedPrebookingWindow.value))
        newVm.isHideExactTimes.set(trip.isHideExactTimes || trip.segments.any { it.isHideExactTimes })
        accessibilityLabel.set(getAccessibilityLabel() ?: context.getString(R.string.book))
        setSegments(newVm.segments, trip)

        return newVm
    }

    private fun getAccessibilityLabel(): String? {
        var mAccessibilityLabel: String? = null
        trip.segments?.forEach {
            if (!it.booking?.accessibilityLabel.isNullOrEmpty()) {
                mAccessibilityLabel = it.booking?.accessibilityLabel
            }
        }
        return mAccessibilityLabel
    }

    private fun buildContentDescription(trip: Trip): String? {
        val contentDescBuilder = StringBuilder()
        trip.segments.forEach {
            if (it.modeInfo != null) {
                contentDescBuilder.append(it.modeInfo?.alternativeText).append(" ")
                contentDescBuilder.append(it.modeInfo?.description).append(" ")
                contentDescBuilder.append("for ").append(buildTitle(context, trip).replace(context.getString(R.string.str_mins), context.getString(R.string.str_minutes))).append(". ")
            }
            if (it == trip.segments.last()) {
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
                Triple(R.drawable.ic_money_sign_circle, R.string.cheapest, R.color.classification_cheapest)
            }
            TripGroupClassifier.Classification.FASTEST -> {
                Triple(R.drawable.ic_lightning_circle, R.string.fastest, R.color.classification_fastest)
            }
            TripGroupClassifier.Classification.GREENEST -> {
                Triple(R.drawable.ic_leaf_circle, R.string.greenest, R.color.classification_greenest)
            }
            TripGroupClassifier.Classification.HEALTHIEST -> {
                Triple(R.drawable.ic_heart_circle, R.string.healthiest, R.color.classification_healthiest)
            }
            TripGroupClassifier.Classification.RECOMMENDED -> {
                Triple(R.drawable.ic_recommeneded_circle, R.string.recommended, R.color.classification_recommended)
            }
            else -> {
                Triple(-1, -1, -1)
            }
        }
        if (drawableRes != -1) {
            badgeDrawable.set(ContextCompat.getDrawable(context, drawableRes))
            badgeText.set(context.getString(textRes))
            badgeTextColor.set(ContextCompat.getColor(context, textColor))
            badgeVisible.set(true)
        }
    }


    private fun buildTitle(context: Context, _trip: Trip): String {
        return if (_trip.isDepartureTimeFixed) {
            showTimeRange(_trip.startDateTime, _trip.endDateTime)
        } else {
            formatDuration(context, _trip.startTimeInSecs, _trip.endTimeInSecs)
        }
    }

    /**
     * For example, '09:40am - 10:40am (59mins)'
     */
    private fun showTimeRange(startDateTime: DateTime, endDateTime: DateTime): String {
        return "${printTime.printLocalTime(startDateTime.toLocalTime())} - ${printTime.printLocalTime(endDateTime.toLocalTime())}"
    }

    /**
     * For example, 1hr 50mins
     */
    private fun formatDuration(context: Context, startTimeInSecs: Long, endTimeInSecs: Long): String = TimeUtils.getDurationInDaysHoursMins(context, (endTimeInSecs - startTimeInSecs).toInt())

    private fun buildSubtitle(context: Context, _trip: Trip): String {
        return if (_trip.isDepartureTimeFixed) {
            formatDuration(context, _trip.startTimeInSecs, _trip.endTimeInSecs)
        } else if (!_trip.queryIsLeaveAfter()) {
            resources.getString(R.string.departs__pattern, printTime.printLocalTime(_trip.startDateTime.toLocalTime()))
        } else {
            resources.getString(R.string.arrives__pattern, printTime.printLocalTime(_trip.endDateTime.toLocalTime()))
        }
    }

    private fun setSegments(_segments: ArrayList<TripSegmentViewModel>, _trip: Trip) {
        _segments.clear()
        _trip.getSummarySegments().forEach { segment ->
            val newModel = TripSegmentViewModel(context, printTime)
            newModel.setSegment(_trip, segment)
            _segments.add(newModel)
        }
    }


    private fun setCost() {
        var displayCost = trip.getDisplayCost(resources.getString(R.string.free))
        var displayCarbon = trip.displayCarbonCost
        var displayCalories = trip.displayCalories
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

        cost.set(builder.toString())

        val globalConfigs = TripKit.getInstance().configs()
        costVisible.set(!globalConfigs.hideTripMetrics())
        hasTripLabels.set(globalConfigs.hasTripLabels())
    }
}