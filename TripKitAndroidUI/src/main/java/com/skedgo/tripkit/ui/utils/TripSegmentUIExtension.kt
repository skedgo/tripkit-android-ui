package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.google.android.gms.common.util.CollectionUtils
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.endDateTime
import com.skedgo.tripkit.routing.startDateTime
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummary
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
import android.text.format.DateFormat
import com.skedgo.tripkit.ui.utils.SystemTimeFormatManager


fun TripSegment.getSegmentIconObservable(
    context: Context,
    getTransportIconTintStrategy: GetTransportIconTintStrategy,
): Observable<Drawable?> {
    val url = TransportModeUtils.getIconUrlForModeInfo(context.resources, this.modeInfo)
    var remoteIcon = Observable.empty<Drawable>()
    if (url != null) {
        remoteIcon = TripKitUI.getInstance().picasso().fetchAsync(url).toObservable()
            .map { bitmap -> BitmapDrawable(context.resources, bitmap) }
    }

    return Observable
        .just(getLocalSegmentIcon(context))
        .concatWith(remoteIcon)
        .doOnError { e -> Timber.e(e) }
        .flatMap { drawable ->
            getTransportIconTintStrategy.invoke()
                .map { transportTintStrategy ->
                    transportTintStrategy.apply(
                        this.modeInfo?.remoteIconIsTemplate ?: false,
                        this.modeInfo?.remoteIconIsBranding ?: false,
                        this.serviceColor,
                        drawable
                    )
                }
                .toObservable()
        }.observeOn(AndroidSchedulers.mainThread())
}

internal fun TripSegment.getLocalSegmentIcon(context: Context): Drawable {
    // Persisted trips can contain an absent or no-longer-recognised local icon name.
    // Their mode, times and instructions are still usable: show a real fallback icon
    // rather than emitting null (RxJava forbids it) or dropping the summary entry.
    val localIconResource = darkVehicleIcon.takeIf { it != 0 }
        ?: TransportMode.getLocalIconResId(transportModeId).takeIf { it != 0 }
        ?: if (getType() == com.skedgo.tripkit.routing.SegmentType.ARRIVAL ||
            getType() == com.skedgo.tripkit.routing.SegmentType.DEPARTURE) {
            R.drawable.v4_ic_map_location
        } else TransportMode.getLocalIconResId(TransportMode.ID_PUBLIC_TRANSPORT)

    if (darkVehicleIcon == 0) {
        Timber.w("Segment icon fallback: mode=%s localIcon=%s type=%s",
            transportModeId, modeInfo?.localIconName, getType())
    }

    return requireNotNull(ContextCompat.getDrawable(context, localIconResource))
}

fun TripSegment.createSummaryIcon(
    context: Context,
    transportIcon: Drawable
): Drawable {
    transportIcon.setBounds(0, 0, transportIcon.intrinsicWidth, transportIcon.intrinsicHeight)
    if (CollectionUtils.isEmpty(this.alerts) || shouldAttachAlertIconToSubtitle()) {
        return transportIcon
    }

    val alertIcon = this.getAlertIcon(context)
    val layers = arrayOf(transportIcon, alertIcon)

    val layerDrawable = LayerDrawable(layers)
    layerDrawable.setBounds(0, 0, transportIcon.intrinsicWidth, transportIcon.intrinsicHeight)
    alertIcon?.setBounds(
        0,
        transportIcon.intrinsicHeight / 4,
        transportIcon.intrinsicWidth / 4 * 3,
        transportIcon.intrinsicHeight
    )
    return layerDrawable
}

private fun TripSegment.getAlertIcon(context: Context): Drawable? {
    val iconRes = if (RealtimeAlert.SEVERITY_ALERT == this.alerts?.firstOrNull()?.severity())
        R.drawable.ic_alert_red_overlay
    else
        R.drawable.ic_alert_yellow_overlay
    return ContextCompat.getDrawable(context, iconRes)
}

private fun TripSegment.shouldAttachAlertIconToSubtitle(): Boolean {
    return (serviceTripId == null
        && startStopCode == null
        && endStopCode == null)
}

fun TripSegment.generateTripPreviewHeader(icon: Drawable): TripSegmentSummary {
    val timePattern = SystemTimeFormatManager.getTimeFormatPattern()
    val dateTimeFormatter = DateTimeFormat.forPattern(timePattern)
    return TripSegmentSummary(
        id = this.segmentId,
        title = this.getTitle(),
        subTitle = this.endDateTime.toString(dateTimeFormatter),
        icon = icon,
        description = "${
            this.trip?.startDateTime?.toString(
                dateTimeFormatter
            )
        } - ${this.trip?.endDateTime?.toString(dateTimeFormatter)}",
        modeId = this.transportModeId,
        isHideExactTimes = this.isHideExactTimes
    )
}

fun TripSegment.generateTripPreviewHeader(
    context: Context,
    icon: Drawable,
    printTime: PrintTime
): TripSegmentSummary {
    val timePattern = SystemTimeFormatManager.getTimeFormatPattern()
    val dateTimeFormatter = DateTimeFormat.forPattern(timePattern)
    return TripSegmentSummary(
        id = this.segmentId,
        title = this.getTitle(),
        subTitle = this.getSubtitle(context, this.trip, printTime),
        icon = icon,
        description = "${
            this.trip?.startDateTime?.toString(
                dateTimeFormatter
            )
        } - ${this.trip?.endDateTime?.toString(dateTimeFormatter)}",
        modeId = this.transportModeId,
        isHideExactTimes = this.isHideExactTimes
    )
}

private fun TripSegment.getSubtitle(context: Context, trip: Trip?, printTime: PrintTime): String? {
    when {
        this.isRealTime -> {
            return if (this.hasTimeTable()) {
                if (this.frequency == 0) {
                    printTime.printLocalTime(this.startDateTime.toLocalTime())
                } else {
                    context.resources.getString(R.string.real_minustime)
                }
            } else {
                context.resources.getString(R.string.live_traffic)
            }
        }

        trip?.isMixedModal(false) == true && !this.hasTimeTable() -> {
            return TimeUtils.getDurationInHoursMins(
                context,
                (this.endTimeInSecs - this.startTimeInSecs).toInt()
            )
        }

        this.metresSafe > 0 -> {
            if (this.isCycling) {
                return context.resources.getString(
                    R.string._pattern_cycle_friendly,
                    "${this.cycleFriendliness}%"
                )
            } else if (this.isWheelchair) {
                return context.resources.getString(
                    R.string._pattern_wheelchair_friendly,
                    "${this.wheelchairFriendliness}%"
                )
            }
        }

        this.hasTimeTable() -> {
            if (this.frequency == 0) {
                return printTime.printLocalTime(this.startDateTime.toLocalTime())
            }
        }
    }

    return null
}

private fun TripSegment.getTitle(): String {
    return when {
        !this.serviceNumber.isNullOrEmpty() -> {
            this.serviceNumber!!
        }

        !this.modeInfo?.description.isNullOrBlank() -> {
            this.modeInfo?.description ?: ""
        }

        this.transportModeId != TransportMode.ID_WALK -> {
            DistanceFormatter.format(this.metres, getDistanceRegion())
        }

        else -> {
            ""
        }
    }
}

internal fun TripSegment.getDistanceRegion(): String? =
    from?.region
        ?: to?.region
        ?: singleLocation?.region
        ?: trip?.from?.region
        ?: trip?.to?.region

private fun TripSegment.getTimeText(context: Context): String {
    val timePattern = SystemTimeFormatManager.getTimeFormatPattern()
    val dateTimeFormatter = DateTimeFormat.forPattern(timePattern)
    return startDateTime.toString(dateTimeFormatter)
}

private fun TripSegment.getTimeRangeText(context: Context): String {
    val timePattern = SystemTimeFormatManager.getTimeFormatPattern()
    val dateTimeFormatter = DateTimeFormat.forPattern(timePattern)
    return "${startDateTime.toString(dateTimeFormatter)} - ${endDateTime.toString(dateTimeFormatter)}"
}
