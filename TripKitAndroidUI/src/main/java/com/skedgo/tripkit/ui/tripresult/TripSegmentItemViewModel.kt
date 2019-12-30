package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.tripresults.TripSegmentHelper
import com.skedgo.tripkit.ui.utils.TripSegmentActionProcessor
import com.skedgo.tripkit.ui.utils.tint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
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

    val title = ObservableField<String>()
    val startTime = ObservableField<String>()
    val showStartTime = ObservableBoolean(false)
    val endTime = ObservableField<String>()
    val showEndTime = ObservableBoolean(false)


    val description = ObservableField<String>()
    val showDescription = ObservableBoolean(false)
    val icon = ObservableField<Drawable>()
    val showBackgroundCircle = ObservableBoolean(false)
    val backgroundCircleTint = ObservableField<Int>(Color.TRANSPARENT)

    val topLineTint = ObservableField<Int>(Color.TRANSPARENT)
    val bottomLineTint = ObservableField<Int>(Color.TRANSPARENT)
    val showTopLine = ObservableBoolean(false)
    val showBottomLine = ObservableBoolean(false)
    var tripSegment: TripSegment? = null

    fun setupSegment(viewType: SegmentViewType,
                     title: String,
                     description: String? = null,
                     startTime: String? = null,
                     endTime: String? = null,
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

            if (startTime != null) {
                this.startTime.set(startTime)
                this.showStartTime.set(true)
            }

            if (endTime != null && endTime != startTime) {
                this.endTime.set(endTime)
                this.showEndTime.set(true)
            }

            if (lineColor != Color.TRANSPARENT) {
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

            if (segmentCircleColor != Color.TRANSPARENT && lineColor == Color.TRANSPARENT) {
                val drawable = ContextCompat.getDrawable(context, R.drawable.segment_circle)
                drawable?.let {
                    val d = it.mutate()
                    d.setColorFilter(segmentCircleColor, PorterDuff.Mode.ADD)
                    icon.set(d)
                }
            }

            if (viewType == SegmentViewType.TERMINAL) {
                if (lineColor == Color.TRANSPARENT) {
                    showSegmentIcon(it, tintWhite)
                }
            } else {
                if (segmentCircleColor == Color.TRANSPARENT || lineColor != Color.TRANSPARENT){
                    showSegmentIcon(it, tintWhite)
                }
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

    private fun showSegmentDescription(segment: TripSegment, processor: TripSegmentActionProcessor) {
        if (!TextUtils.isEmpty(segment.notes)) {
            showDescription.set(true)
            description.set(processor.processText(context, segment, segment.getDisplayNotes(context.resources)!!, true))
        } else {
            showDescription.set(false)
        }
    }
//    private fun processedActionTitle(segment: TripSegment, processor: TripSegmentActionProcessor): String {
//        return if (!segment.action.isNullOrBlank()) {
//            processor.processText(context, segment, segment.action!!, false)
//        } else {
//            String()
//        }
//    }
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