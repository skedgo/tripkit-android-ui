package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.util.CollectionUtils
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber

class TripPreviewPagerViewModel : RxViewModel() {

    private val _headers = MutableLiveData<List<TripPreviewHeader>>()
    val headers: LiveData<List<TripPreviewHeader>> = _headers

    fun generatePreviewHeaders(
            context: Context,
            tripSegments: List<TripSegment>,
            getTransportIconTintStrategy: GetTransportIconTintStrategy
    ) {

        val previewHeaders = mutableListOf<TripPreviewHeader>()

        tripSegments.forEach { segment ->
            getSegmentIcon(context, segment, getTransportIconTintStrategy) {
                previewHeaders.add(
                        TripPreviewHeader(
                                title = getTitle(segment),
                                icon = it
                        )
                )
                _headers.value = previewHeaders
            }
        }

    }

    private fun getSegmentIcon(
            context: Context,
            segment: TripSegment,
            getTransportIconTintStrategy: GetTransportIconTintStrategy,
            icon: (Drawable) -> Unit
    ) {

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
                .just(ContextCompat.getDrawable(context, segment.darkVehicleIcon))
                .concatWith(remoteIcon)
                .doOnError { e -> Timber.e(e) }
                .flatMap { drawable ->
                    getTransportIconTintStrategy.invoke()
                            .map { transportTintStrategy -> transportTintStrategy.apply(segment.modeInfo!!.remoteIconIsTemplate, segment.modeInfo!!.remoteIconIsBranding, segment.serviceColor, drawable) }
                            .toObservable()
                }
                .map { bitmapDrawable ->
                    createSummaryIcon(context, segment, bitmapDrawable)
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    icon.invoke(it)
                }.autoClear()
    }

    private fun createSummaryIcon(context: Context, segment: TripSegment, transportIcon: Drawable): Drawable {
        transportIcon.setBounds(0, 0, transportIcon.intrinsicWidth, transportIcon.intrinsicHeight)
        if (CollectionUtils.isEmpty(segment.alerts) || shouldAttachAlertIconToSubtitle(segment)) {
            return transportIcon
        }

        val alertIcon = getAlertIcon(context, segment)
        val layers = arrayOf(transportIcon, alertIcon)

        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setBounds(0, 0, transportIcon.intrinsicWidth, transportIcon.intrinsicHeight)
        alertIcon?.setBounds(0,
                transportIcon.intrinsicHeight / 4,
                transportIcon.intrinsicWidth / 4 * 3,
                transportIcon.intrinsicHeight
        )
        return layerDrawable
    }

    private fun getAlertIcon(context: Context, segment: TripSegment): Drawable? {
        val iconRes = if (RealtimeAlert.SEVERITY_ALERT == segment.alerts!![0].severity())
            R.drawable.ic_alert_red_overlay
        else
            R.drawable.ic_alert_yellow_overlay
        return ContextCompat.getDrawable(context, iconRes)
    }

    private fun shouldAttachAlertIconToSubtitle(segment: TripSegment): Boolean {
        return (segment.serviceTripId == null
                && segment.startStopCode == null
                && segment.endStopCode == null)
    }

    private fun getTitle(segment: TripSegment): String {
        return if (!TextUtils.isEmpty(segment.serviceNumber)) {
            segment.serviceNumber
        } else {
            segment.modeInfo?.description ?: ""
        }
    }

}