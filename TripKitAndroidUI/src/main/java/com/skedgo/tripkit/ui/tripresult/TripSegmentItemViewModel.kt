package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface.BOLD
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.common.model.Booking
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.tripresults.TripSegmentHelper
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject


class TripSegmentItemViewModel @Inject internal constructor(private val context: Context,
                                                    private val getTransportIconTintStrategy: GetTransportIconTintStrategy,
                                                    private val tripSegmentHelper: TripSegmentHelper,
                                                    private val printTime: PrintTime)
    : RxViewModel() {
    enum class SegmentViewType {
        TERMINAL,
        STATIONARY,
        STATIONARY_BRIDGE,
        MOVING
    }
    val onClick = TapAction.create<TripSegmentItemViewModel>() { this }
    val title = ObservableField<String>()
    val startTime = ObservableField<SpannableString>()
    val showStartTime = ObservableBoolean(false)
    val endTime = ObservableField<SpannableString>()
    val showEndTime = ObservableBoolean(false)
    val startTimeColor = ObservableField<Int>(ContextCompat.getColor(context, R.color.black1))

    val description = ObservableField<String>()
    val showDescription = ObservableBoolean(false)
    val icon = ObservableField<Drawable>()
    val showBackgroundCircle = ObservableBoolean(false)
    val backgroundCircleTint = ObservableField<Int>(Color.TRANSPARENT)

    val topLineTint = ObservableField<Int>(Color.TRANSPARENT)
    val bottomLineTint = ObservableField<Int>(Color.TRANSPARENT)
    val showTopLine = ObservableBoolean(false)
    val showBottomLine = ObservableBoolean(false)

    val showAlerts = ObservableBoolean(false)
    val alerts = ObservableField<ArrayList<RealtimeAlert>>()

    val alertsClicked = BehaviorRelay.create<ArrayList<RealtimeAlert>>()
    var tripSegment: TripSegment? = null

    val externalAction = ObservableField<String>()
    val externalActionClicked = BehaviorRelay.create<TripSegment>()

    fun setupSegment(viewType: SegmentViewType,
                     title: String,
                     description: String? = null,
                     startTime: String? = null,
                     endTime: String? = null,
                     delay: Long = 0,
                     hasRealtime: Boolean = false,
                     lineColor: Int = Color.TRANSPARENT,
                     topConnectionColor: Int = lineColor,
                     bottomConnectionColor: Int = lineColor) {
        var tintWhite = false
        tripSegment?.let {
            this.title.set(title)
            if (description != null) {
                this.description.set(description)
                this.showDescription.set(true)
            }

            if ((it.correctItemType() == ITEM_EXTERNAL_BOOKING && viewType == SegmentViewType.MOVING)
                || (it.correctItemType() == ITEM_NEARBY && it.booking?.externalActions?.isNotEmpty() == true)){
                externalAction.set(it.booking!!.title)
            }

            if (hasRealtime) {
                // We show the realtime times differently than when they aren't real time. When a
                // segment is on time, it is shown in bold green text. If it is early, the top text is a bold warning
                // color showing the realtime arrival time, and the bottom text shows the planned time crossed out.
                // Similarly, we do the same in red when a service is late.

                val startTimeSpannable = SpannableString(startTime)
                startTimeSpannable.setSpan(StyleSpan(BOLD),
                        0,
                        startTimeSpannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                if (delay > 0) { // Late
                    startTimeSpannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.tripKitError)),
                            0,
                            startTimeSpannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                } else if (delay < 0) { // Early
                    startTimeSpannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.tripKitWarning)),
                            0,
                            startTimeSpannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else { // On time
                    startTimeSpannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.tripKitSuccess)),
                            0,
                            startTimeSpannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                this.startTime.set(startTimeSpannable)
                this.showStartTime.set(true)

                if (endTime != null) {
                    // This is not the end time, but rather the timetable time for realtime services. It is shown
                    // crossed out.
                    val endTimeSpannable = SpannableString(endTime)
                    endTimeSpannable.setSpan(StrikethroughSpan(),
                            0,
                            endTimeSpannable.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    this.endTime.set(endTimeSpannable)
                    this.showEndTime.set(true)
                }
            } else {
                if (startTime != null) {
                    this.startTime.set(SpannableString(startTime))
                    this.showStartTime.set(true)
                }

                if (endTime != null && endTime != startTime) {
                    this.endTime.set(SpannableString(endTime))
                    this.showEndTime.set(true)
                }
            }

            // When showing transit lines, we draw a circle in the middle with the icon overlayed on it and lines connecting
            // the top and/or bottom segments.
            // For the start/destination parts, usually a STATIONARY_BRIDGE view, a small white circle with the line color's border is used.
            // Those STATIONARY_BRIDGE views set the lineColor to transparent and just use a connection color, so
            // we also handle the special case of a TERMINAL being the last segment.
            if (lineColor != Color.TRANSPARENT
                    && viewType != SegmentViewType.TERMINAL /* Don't show the circle background when it's a terminal */) {
                tintWhite = true
                backgroundCircleTint.set(lineColor)
                showBackgroundCircle.set(true)
            }

            var segmentCircleColor = Color.TRANSPARENT
            if (topConnectionColor != Color.TRANSPARENT){
                this.topLineTint.set(topConnectionColor)
                this.showTopLine.set(true)
                segmentCircleColor = topConnectionColor
            }

            if (bottomConnectionColor != Color.TRANSPARENT) {
                this.bottomLineTint.set(bottomConnectionColor)
                this.showBottomLine.set(true)
                if (segmentCircleColor != Color.TRANSPARENT) {
                    segmentCircleColor = Color.GRAY
                } else {
                    segmentCircleColor = bottomConnectionColor
                }
            }

            // If the terminal segment is the end of a public transit line, end with that rather than a destination icon.
            if (segmentCircleColor != Color.TRANSPARENT && (lineColor == Color.TRANSPARENT || viewType == SegmentViewType.TERMINAL)) {
                val drawable = ContextCompat.getDrawable(context, R.drawable.segment_circle)
                drawable?.let {
                    val d = it.mutate()
                    d.setColorFilter(segmentCircleColor, PorterDuff.Mode.ADD)
                    icon.set(d)
                }
            }

            if (viewType == SegmentViewType.TERMINAL) {
                // But terminal segments that are not part of a transit line show the map marker icon.
                if (lineColor == Color.TRANSPARENT) {
                    showSegmentIcon(it, tintWhite)
                }
            } else {
                // Everything else with a linecolor shows the representative icon.
                if (segmentCircleColor == Color.TRANSPARENT || lineColor != Color.TRANSPARENT){
                    showSegmentIcon(it, tintWhite)
                }
            }

            if (!it.alerts.isNullOrEmpty()) {
                showAlerts.set(true)
                alerts.set(it.alerts)
            }
        }
    }
    private fun serviceColor(): Int {
        tripSegment?.serviceColor?.let {
            return when (it.color) {
                Color.BLACK, Color.WHITE -> Color.BLACK
                else -> it.color
            }
        }
        return Color.TRANSPARENT

    }

    fun onAlertClick(view: View) {
        alertsClicked.accept(alerts.get())
    }

    fun onExternalActionClicked(view : View) {
        tripSegment?.let {
            externalActionClicked.accept(it)
        }
    }
    protected fun showSegmentIcon(segment: TripSegment, tintWhite: Boolean) {
        if (segment.type == SegmentType.ARRIVAL || segment.type == SegmentType.DEPARTURE) {
            icon.set(ContextCompat.getDrawable(context, R.drawable.v4_ic_map_location))
        } else {
            if (segment.modeInfo == null || segment.modeInfo!!.modeCompat == null) {
                icon.set(null)
            } else {
                val url = tripSegmentHelper.getIconUrlForModeInfo(context.resources, segment.modeInfo)
                var remoteIcon = Observable.empty<Drawable>()
                if (url != null) {
                    remoteIcon = TripKitUI.getInstance().picasso().fetchAsync(url).toObservable()
                            .map { bitmap -> BitmapDrawable(context.resources, bitmap) }
                }
                Observable
                        .just(ContextCompat.getDrawable(context, segment.darkVehicleIcon))
                        .concatWith(remoteIcon)
                        .map {
                            if (tintWhite) {
                                it.tint(Color.WHITE)
                            } else {
                                it
                            }
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ drawable:
                                    Drawable -> icon.set(drawable)
                        }, { e -> Timber.e(e) }).autoClear()

            }
        }
    }

}