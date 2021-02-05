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
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.joda.time.DateTime
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.utils.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TripResultTripViewModel: ViewModel() {
    var trip: Trip? = null
    val title = ObservableField<String>()
    val subtitle = ObservableField<String>()
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

class TripResultViewModel  @Inject constructor(private val context: Context,
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

    var actionButtonHandler: ActionButtonHandler? = null

    // Footer
    val alternateTripVisible = ObservableBoolean(false)
    val costVisible = ObservableBoolean(true)
    val cost = ObservableField<String>()
    val moreButtonVisible = ObservableBoolean(false)
    var moreButtonText = ObservableField<String>()
    var otherTripGroups : List<Trip>? = null
    var classification = TripGroupClassifier.Classification.NONE

    override fun equals(other: Any?) : Boolean {
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
                     classification: TripGroupClassifier.Classification?)  {
        moreButtonText.set(context.resources.getString(R.string.more))
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
        }
        setCost()

        val actionButtonText = actionButtonHandler?.getPrimaryAction(context, trip)

        actionButtonText?.let {
            moreButtonText = actionButtonText
            moreButtonVisible.set(true)
        }
    }

    private fun addTripToList(trip: Trip) {
        val newVm = TripResultTripViewModel()
        newVm.trip = trip
        newVm.clickFlow = clickFlow
        newVm.title.set(buildTitle(trip))
        newVm.subtitle.set(buildSubtitle(trip))
        setSegments(newVm.segments, trip)
        tripResults.add(newVm)
    }

    private fun setBadge(classification: TripGroupClassifier.Classification) {
        this.classification = classification
        val (drawableRes, textRes, textColor) = when (classification) {
            TripGroupClassifier.Classification.EASIEST -> {  Triple(R.drawable.ic_like_circle, R.string.easiest, R.color.classification_easiest) }
            TripGroupClassifier.Classification.CHEAPEST -> {  Triple(R.drawable.ic_money_sign_circle, R.string.cheapest, R.color.classification_cheapest) }
            TripGroupClassifier.Classification.FASTEST -> {  Triple(R.drawable.ic_lightning_circle,R.string.fastest, R.color.classification_fastest) }
            TripGroupClassifier.Classification.GREENEST -> {  Triple(R.drawable.ic_leaf_circle, R.string.greenest, R.color.classification_greenest) }
            TripGroupClassifier.Classification.HEALTHIEST -> {  Triple(R.drawable.ic_heart_circle, R.string.healthiest, R.color.classification_healthiest) }
            TripGroupClassifier.Classification.RECOMMENDED -> {  Triple(R.drawable.ic_recommeneded_circle, R.string.recommended, R.color.classification_recommended) }
            else -> { Triple(-1, -1, -1)}
        }
        if (drawableRes != -1) {
            badgeDrawable.set(ContextCompat.getDrawable(context, drawableRes))
            badgeText.set(context.getString(textRes))
            badgeTextColor.set(ContextCompat.getColor(context, textColor))
            badgeVisible.set(true)
        }
    }


    private fun buildTitle(_trip: Trip) : String {
        return if (_trip.isDepartureTimeFixed) {
            showTimeRange(_trip.startDateTime, _trip.endDateTime)
        } else {
            formatDuration(_trip.startTimeInSecs, _trip.endTimeInSecs)
        }
    }
    /**
     * For example, '09:40am - 10:40am (59mins)'
     */
    private fun showTimeRange(startDateTime: DateTime, endDateTime: DateTime) : String {
        return "${printTime.printLocalTime(startDateTime.toLocalTime())} - ${printTime.printLocalTime(endDateTime.toLocalTime())}"
    }

    /**
     * For example, 1hr 50mins
     */
    private fun formatDuration(startTimeInSecs: Long, endTimeInSecs: Long): String  = TimeUtils.getDurationInDaysHoursMins((endTimeInSecs - startTimeInSecs).toInt())

    private fun buildSubtitle(_trip: Trip): String {
        return if (_trip.isDepartureTimeFixed) {
            formatDuration(_trip.startTimeInSecs, _trip.endTimeInSecs)
        } else if (!_trip.queryIsLeaveAfter()){
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
    }
}