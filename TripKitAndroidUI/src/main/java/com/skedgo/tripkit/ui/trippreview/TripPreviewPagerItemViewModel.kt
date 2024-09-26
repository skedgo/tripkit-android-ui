package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.generic.transport.TransportDetails
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import com.skedgo.tripkit.ui.utils.TapAction
import com.skedgo.tripkit.ui.utils.TapStateFlow
import com.skedgo.tripkit.ui.utils.checkDateForStringLabel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
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
    val modeTitle = ObservableField<String>()
    val modeIconUrl = ObservableField<String>()

    var segment: TripSegment? = null

    val externalActionChosen = PublishRelay.create<Action>()
    val enableActionButtons = ObservableBoolean(true)

    open fun setSegment(context: Context, segment: TripSegment) {
        this.segment = segment
        this.modeTitle.set(getModeTitle(segment))
        title.set(TripSegmentUtils.getTripSegmentAction(context, segment) ?: "Unknown Action")
        var title = segment.miniInstruction?.instruction ?: title.get()
        title = try {
            title?.replace(Templates.TEMPLATE_PLATFORM, segment.platform ?: "")?.replace(
                SegmentActionTemplates.TEMPLATE_NUMBER, segment.serviceNumber
            )
        } catch (e: Exception) {
            title
        }
        instructionTitle.set(title)

        var instruction = segment.miniInstruction?.description
        instruction = try {
            instruction?.replace(
                SegmentNotesTemplates.TEMPLATE_DIRECTION,
                segment.serviceDirection ?: ""
            )
        } catch (e: Exception) {
            instruction
        }

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
        modeIconUrl.set(url)
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
        if (segment.isHideExactTimes) {
            if (segment.trip.queryTime > 0) {
                val queryDateTime = segment.trip.queryDateTime
                val date = queryDateTime.toString(DateTimeFormat.forPattern("MMM d, yyyy"))
                val time = queryDateTime.toString(DateTimeFormat.forPattern("h:mm aa"))
                val label = String.format(context.getString(R.string.requested_time), date, time)
                if (segment.trip.queryIsLeaveAfter()) {
                    requestedPickUp.set(label)
                } else {
                    requestedDropOff.set(label)
                }
            }
        } else {
            val startDateTime = segment.startDateTime
            val labelForStartDate = startDateTime.toDate().checkDateForStringLabel(context)
            val startDate = labelForStartDate
                ?: startDateTime.toString(DateTimeFormat.forPattern("MMM d, yyyy"))
            val startTime = startDateTime.toString(DateTimeFormat.forPattern("h:mm aa"))

            val endDateTime = segment.endDateTime
            val labelForEndDate = endDateTime.toDate().checkDateForStringLabel(context)
            val endDate = labelForEndDate
                ?: endDateTime.toString(DateTimeFormat.forPattern("MMM d, yyyy"))
            val endTime = endDateTime.toString(DateTimeFormat.forPattern("h:mm aa"))

            requestedPickUp.set("$startDate $startTime")
            requestedDropOff.set("$endDate $endTime")
        }

        if (segment.trip.queryTime > 0) {
            fetchRegionAndSetupPickUpMessage(segment.trip)
        }

        hasPickUpWindow.set(
            segment.booking?.confirmation?.purchase()?.pickupWindowDuration() != null
        )
    }

    private fun fetchRegionAndSetupPickUpMessage(trip: Trip) {
        TripKitUI.getInstance().regionService().getRegionByLocationAsync(trip.from)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                getPickUpWindowMessage(trip, it)
            }, {
                Timber.e(it)
                if (BuildConfig.DEBUG) {
                    it.printStackTrace()
                }
                getPickUpWindowMessage(trip, null)
            }).autoClear()
    }

    private fun getPickUpWindowMessage(trip: Trip, region: Region?) {
        val dateTime = trip.startDateTime

        val timeZone: String? = region?.timezone ?: trip.segments.first().timeZone

        val date = dateTime.toString(
            DateTimeFormat.forPattern("MMM d, yyyy")
                .withZone(DateTimeZone.forID(timeZone))
        )
        val time = dateTime.toString(
            DateTimeFormat.forPattern("h:mm aa")
                .withZone(DateTimeZone.forID(timeZone))
        )

        pickUpWindowMessage.set(String.format("Starts at %s at %s", date, time))
    }

    fun getModeTitle(segment: TripSegment): String {
        return when {
            !TextUtils.isEmpty(segment.serviceNumber) -> {
                segment.serviceNumber
            }
            !segment.modeInfo?.description.isNullOrBlank() -> {
                segment.modeInfo?.description ?: ""
            }
            segment.transportModeId != TransportMode.ID_WALK -> {
                DistanceFormatter.format(segment.metres)
            }
            else -> {
                ""
            }
        }
    }

    fun generateTransportDetails(): TransportDetails {
        return TransportDetails(
            fromLocation.get() ?: "",
            requestedPickUp.get() ?: "",
            toLocation.get() ?: "",
            requestedDropOff.get() ?: ""
        )
    }
}