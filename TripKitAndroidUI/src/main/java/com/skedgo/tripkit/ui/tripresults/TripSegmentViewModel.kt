package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.google.android.gms.common.util.CollectionUtils
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.fetchAsync
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.startDateTime
import timber.log.Timber
import javax.inject.Inject


class TripSegmentViewModel @Inject constructor(private val context: Context, private val printTime: PrintTime): RxViewModel() {
    @Inject
    lateinit var getTransportIconTintStrategy: GetTransportIconTintStrategy

    init {
        TripKitUI.getInstance().tripSegmentViewModelComponent().inject(this)
    }
    val icon = ObservableField<Drawable>()

    val showPrimary = ObservableBoolean(false)
    val primaryText = ObservableField<String>()
    val showSecondary = ObservableBoolean(false)
    val secondaryText = ObservableField<String>()

    fun setSegment(trip: Trip, segment: TripSegment) {
        showTitle(segment)

        val subtitle = getSubtitle(trip, segment)
        if (!TextUtils.isEmpty(subtitle)) {
            secondaryText.set(subtitle)
            showSecondary.set(true)
        }

        if (segment.modeInfo == null || segment.modeInfo!!.localIconName == null) {
            return
        }
        if (segment.darkVehicleIcon == 0) {
            return
        }
        val url = TransportModeUtils.getIconUrlForModeInfo(context.resources, segment.modeInfo)
        var remoteIcon = Observable.empty<Drawable>()
        if (url != null) {
            remoteIcon = TripKitUI.getInstance().picasso().fetchAsync(url).toObservable()
                    .map { bitmap -> BitmapDrawable(context.resources, bitmap) }
        }
        Observable
            .just(context.resources.getDrawable(segment.darkVehicleIcon))
            .concatWith(remoteIcon)
            .doOnError { e -> Timber.e(e) }
            .flatMap { drawable ->
                getTransportIconTintStrategy.invoke()
                        .map { transportTintStrategy -> transportTintStrategy.apply(segment.modeInfo!!.remoteIconIsTemplate, segment.serviceColor, drawable) }
                        .toObservable()
            }
            .map { bitmapDrawable -> createSummaryIcon(segment, bitmapDrawable) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                drawable: Drawable -> icon.set(drawable)
            }, { e -> Timber.e(e) }).autoClear()

    }
    private fun createSummaryIcon(segment: TripSegment, transportIcon: Drawable): Drawable {
        transportIcon.setBounds(0, 0, transportIcon.intrinsicWidth, transportIcon.intrinsicHeight)
        Timber.d("Segment has alerts: %d", segment.alerts?.size)
        if (CollectionUtils.isEmpty(segment.alerts) || shouldAttachAlertIconToSubtitle(segment)) {
            return transportIcon
        }

        val alertIcon = getAlertIcon(segment)
        val layers = arrayOf(transportIcon, alertIcon)

        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setBounds(0, 0, transportIcon.intrinsicWidth, transportIcon.intrinsicHeight)
        alertIcon.setBounds(0,
                transportIcon.intrinsicHeight / 4,
                transportIcon.intrinsicWidth / 4 * 3,
                transportIcon.intrinsicHeight
        )
        return layerDrawable
    }

    private fun getAlertIcon(segment: TripSegment): Drawable {
        val iconRes = if (RealtimeAlert.SEVERITY_ALERT == segment.alerts!![0].severity())
            R.drawable.ic_alert_red_overlay
        else
            R.drawable.ic_alert_yellow_overlay
        return context.resources.getDrawable(iconRes)
    }

    internal fun shouldAttachAlertIconToSubtitle(segment: TripSegment): Boolean {
        return (segment.serviceTripId == null
                && segment.startStopCode == null
                && segment.endStopCode == null)
    }

    private fun showTitle(segment: TripSegment) {
        if (!TextUtils.isEmpty(segment.serviceNumber)) {
            primaryText.set(segment.serviceNumber)
            showPrimary.set(true)
        } else {
            val description = segment.modeInfo?.description
            description?.let {
                if (!TextUtils.isEmpty(it)) {
                    primaryText.set(it)
                    showPrimary.set(true)
                }
            }
        }
    }

    private fun getSubtitle(trip: Trip, segment: TripSegment): String? {
        if (segment.isRealTime) {
            return if (segment.hasTimeTable()) {
                context.resources.getString(R.string.real_minustime)
            } else {
                context.resources.getString(R.string.live_traffic)
            }
        } else if (trip.isMixedModal(false) && !segment.hasTimeTable()) {
            return TimeUtils.getDurationInHoursMins((segment.endTimeInSecs - segment.startTimeInSecs).toInt())
        } else if (segment.metresSafe > 0)  {
            if (segment.isCycling) {
                return context.resources.getString(R.string._pattern_cycle_friendly, "${segment.cycleFriendliness}%")
            } else if (segment.isWheelchair) {
                return context.resources.getString(R.string._pattern_wheelchair_friendly, "${segment.wheelchairFriendliness}%")
            }
        } else if (segment.hasTimeTable()) {
            if (segment.frequency == 0) {
                return printTime.printLocalTime(segment.startDateTime.toLocalTime())
            }
        }

        return null
    }
}