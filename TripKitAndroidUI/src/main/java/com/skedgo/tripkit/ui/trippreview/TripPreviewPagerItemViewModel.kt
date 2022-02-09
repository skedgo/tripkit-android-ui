package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import com.skedgo.tripkit.ui.utils.TapAction
import com.skedgo.tripkit.ui.utils.TapStateFlow
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

open class TripPreviewPagerItemViewModel : RxViewModel() {
    var title = ObservableField<String>()
    var instructionTitle = ObservableField<String>()
    var icon = ObservableField<Drawable>()
    var description = ObservableField<String>()
    var showDescription = ObservableBoolean(true)
    var closeClicked = TapAction { this }
    var notes = ObservableField<String>()
    var messageTitle = ObservableField<String>()
    var message = ObservableField<String>()
    var messageVisible = ObservableBoolean(false)
    var showLaunchInMaps = ObservableBoolean(false)
    var showLaunchInMapsClicked = TapStateFlow { this }
    val fromLocation = ObservableField<String>()
    val toLocation = ObservableField<String>()
    val duration = ObservableField<String>()
    val startDateTime = ObservableField<String>()
    val requestedPickUp = ObservableField<String>()
    val requestedDropOff = ObservableField<String>()
    val hasPickUpWindow = ObservableField<Boolean>()
    val pickUpWindowMessage = ObservableField<String>()

    var segment: TripSegment? = null

    val externalActionChosen = PublishRelay.create<Action>()
    val enableActionButtons = ObservableBoolean(true)

    open fun setSegment(context: Context, segment: TripSegment) {
        this.segment = segment
        title.set(TripSegmentUtils.getTripSegmentAction(context, segment) ?: "Unknown Action")
        instructionTitle.set(segment.miniInstruction?.instruction ?: title.get())
        val instruction = segment.miniInstruction?.description

        if (segment.metres > 0) {
            notes.set(DistanceFormatter.format(segment.metres))
        } else {
            if (segment.notes.equals("<DURATION>", true)) {
                TimeUnit.SECONDS.toMinutes(segment.endTimeInSecs - segment.startTimeInSecs).let {
                    if (it > 0) {
                        notes.set(String.format("%d %s", it, "minutes"))
                    }
                }
            } else {
                notes.set(segment.notes)
            }
        }
        description.set(instruction)
        showDescription.set(!instruction.isNullOrBlank())
        val url = TransportModeUtils.getIconUrlForModeInfo(context.resources, segment.modeInfo)
        var remoteIcon = Observable.empty<Drawable>()
        if (segment.type == SegmentType.ARRIVAL || segment.type == SegmentType.DEPARTURE) {
            icon.set(ContextCompat.getDrawable(context, R.drawable.v4_ic_map_location))
        } else {
            if (segment.modeInfo == null || segment.modeInfo!!.modeCompat == null) {
                val localResource = TransportMode.getLocalIconResId(segment.transportModeId)
                when {
                    localResource > 0 -> {
                        icon.set(ContextCompat.getDrawable(context, localResource))
                    }
                    segment.action?.contains("<TIME>: Wait") == true -> {
                        icon.set(ContextCompat.getDrawable(context, R.drawable.ic_wait))
                    }
                    else -> {
                        icon.set(null)
                    }
                }
            } else {
                if (url != null) {
                    remoteIcon = TripKitUI.getInstance().picasso().fetchAsync(url).toObservable()
                            .map { bitmap -> BitmapDrawable(context.resources, bitmap) }
                }
                Observable
                        .just(ContextCompat.getDrawable(context, segment.darkVehicleIcon))
                        .concatWith(remoteIcon)
                        .map { it }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ drawable:
                                     Drawable ->
                            icon.set(drawable)
                        }, { e -> Timber.e(e) }).autoClear()

            }
        }

        fromLocation.set(segment.from?.address ?: "")
        toLocation.set(segment.to?.address ?: "")

        if (!DateUtils.isToday(segment.startTimeInSecs)) {
            duration.set(segment.startDateTime.toString(DateTimeFormat.forPattern("MMMM dd HH:mm")))
        } else {
            duration.set("Today ${segment.startDateTime.toString(DateTimeFormat.forPattern("HH:mm"))}")
        }

        requestedPickUp.set("")
        requestedDropOff.set("")
        if (segment.trip.queryTime > 0) {
            val queryDateTime = segment.trip.queryDateTime
            val date = queryDateTime.toString(DateTimeFormat.forPattern("MMM d, yyyy"))
            val time = queryDateTime.toString(DateTimeFormat.forPattern("h:mm aa"))
            val label = String.format("Requested time %s at %s", date, time)
            if (segment.trip.queryIsLeaveAfter()) {
                requestedPickUp.set(label)
            } else {
                requestedDropOff.set(label)
            }
            getPickUpWindowMessage(segment.trip, segment.trip.queryIsLeaveAfter())
        }

        hasPickUpWindow.set(false)
        segment.booking?.confirmation?.purchase()?.pickupWindowDuration()?.let {
            hasPickUpWindow.set(true)
        }
    }

    private fun getPickUpWindowMessage(trip: Trip, isLeaveAfter: Boolean) {
        var dateTime = trip.startDateTime
        if (!isLeaveAfter) {
            dateTime = trip.queryDateTime
        }
        val date = dateTime.toString(DateTimeFormat.forPattern("MMM d, yyyy")
                .withZone(DateTimeZone.forID(trip.segments.first().timeZone)))
        val time = dateTime.toString(DateTimeFormat.forPattern("h:mm aa")
                .withZone(DateTimeZone.forID(trip.segments.first().timeZone)))

        pickUpWindowMessage.set(String.format("Starts at %s at %s", date, time))
    }
}