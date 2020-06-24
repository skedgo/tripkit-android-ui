package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.joda.time.DateTime
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.*
import timber.log.Timber
import javax.inject.Inject


class TripResultViewModel  @Inject constructor(private val context: Context,
        private val tripSegmentHelper: TripSegmentHelper,
                                               private val printTime: PrintTime,
                                               private val resources: Resources)
    : RxViewModel() {
    val onItemClicked: TapAction<TripResultViewModel> = TapAction.create { this }
    val onMoreButtonClicked: TapAction<TripResultViewModel> = TapAction.create { this }

    lateinit var group: TripGroup
    lateinit var trip: Trip
    private var alternateTrip: Trip? = null

    // Badge
    val badgeDrawable = ObservableField<Drawable>()
    val badgeText = ObservableField<String>()
    val badgeTextColor = ObservableInt()
    val badgeVisible = ObservableBoolean(false)

    val title = ObservableField<String>()
    val titleVisible = ObservableBoolean(true)
    val subtitle = ObservableField<String>()

    val segments = ArrayList<TripSegmentViewModel>()

    val alternateTitle = ObservableField<String>()
    val alternateTitleVisible = ObservableBoolean(true)
    val alternateSubtitle = ObservableField<String>()
    val alternateSegments = ArrayList<TripSegmentViewModel>()

    // Footer
    val alternateTripVisible = ObservableBoolean(false)
    val costVisible = ObservableBoolean(true)
    val cost = ObservableField<String>()
    val moreButtonVisible = ObservableBoolean(false)
    val moreButtonText = ObservableField<String>()

    var otherTripGroups : List<Trip>? = null

    fun setTripGroup(context: Context, tripgroup: TripGroup, classification: TripGroupClassifier.Classification) {
        moreButtonText.set(context.resources.getString(R.string.more))
        group = tripgroup
        trip = tripgroup.displayTrip!!
        otherTripGroups = tripgroup.trips?.filterNot { it.uuid() == trip.uuid() }
        alternateTrip = otherTripGroups?.firstOrNull()

        if (classification != TripGroupClassifier.Classification.NONE) {
            setBadge(classification)
        }
        buildTitle(trip).subscribe { title.set(it) }.autoClear()
        buildSubtitle(trip).subscribe { subtitle.set(it) }.autoClear()
        setSegments(segments, trip)

        alternateTrip?.let {
            buildTitle(it).subscribe { subtitle -> alternateTitle.set(subtitle) }.autoClear()
            buildSubtitle(it).subscribe { subtitle -> alternateSubtitle.set(subtitle) }.autoClear()
            setSegments(alternateSegments, it)
//          TODO Dynamically show the other trips when the more button is clicked
//            if (otherTripGroups?.size!! > 1) {
//                moreButtonVisible.set(true)
//            }
            alternateTripVisible.set(true)
        }
        setCost()
        val bookingSegment = trip.segments.find { segment -> segment.booking?.quickBookingsUrl != null }
        if (group.frequency > 0) {
            moreButtonText.set(context.getString(R.string.view_times))
            moreButtonVisible.set(true)
        } else if (bookingSegment != null){
            val booking = bookingSegment.booking
            moreButtonText.set(booking.title)
            moreButtonVisible.set(true)
        } else {
            val mainSegment = trip.getMainTripSegment()
            if (mainSegment != null && mainSegment.miniInstruction != null && mainSegment.miniInstruction.instruction != null) {
                moreButtonText.set(mainSegment.miniInstruction.instruction)
                moreButtonVisible.set(true)
            }
        }
        // External booking is missing
        // Turn by turn is missing
        // Main Segment?
    }

    private fun setBadge(classification: TripGroupClassifier.Classification) {
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


    private fun buildTitle(_trip: Trip) : Observable<String> {
        return if (_trip.isDepartureTimeFixed) {
            showTimeRange(_trip.startDateTime, _trip.endDateTime)
        } else {
            Observable.just(formatDuration(_trip.startTimeInSecs, _trip.endTimeInSecs))
        }
    }
    /**
     * For example, '09:40am - 10:40am (59mins)'
     */
    private fun showTimeRange(startDateTime: DateTime, endDateTime: DateTime) : Observable<String> {
        return Observable.combineLatest(printTime.execute(startDateTime).toObservable(), printTime.execute(endDateTime).toObservable(),
                BiFunction { start: String, end: String -> "$start - $end" })
    }

    /**
     * For example, 1hr 50mins
     */
    private fun formatDuration(startTimeInSecs: Long, endTimeInSecs: Long): String  = TimeUtils.getDurationInDaysHoursMins((endTimeInSecs - startTimeInSecs).toInt())

    private fun buildSubtitle(_trip: Trip): Observable<String> {
        if (_trip.isDepartureTimeFixed) {
            return Observable.just(formatDuration(_trip.startTimeInSecs, _trip.endTimeInSecs))
        } else if (!_trip.queryIsLeaveAfter()){
            return printTime.execute(_trip.startDateTime).toObservable().map { resources.getString(R.string.departs__pattern, it) }
        } else {
            return printTime.execute(_trip.endDateTime).toObservable().map { resources.getString(R.string.arrives__pattern, it) }
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