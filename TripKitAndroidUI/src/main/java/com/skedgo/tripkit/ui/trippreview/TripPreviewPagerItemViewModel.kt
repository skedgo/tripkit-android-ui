package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import com.skedgo.tripkit.ui.utils.TapAction
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber

open class TripPreviewPagerItemViewModel : RxViewModel() {
    var title = ObservableField<String>()
    var icon = ObservableField<Drawable>()
    var description = ObservableField<String>()
    var showDescription = ObservableBoolean(true)
    var closeClicked = TapAction<TripPreviewPagerItemViewModel>{ this }
    var notes = ObservableField<String>()
    var messageTitle = ObservableField<String>()
    var message = ObservableField<String>()
    var messageVisible = ObservableBoolean(false)

    open fun setSegment(context: Context, segment: TripSegment) {
        title.set(TripSegmentUtils.getTripSegmentAction(context, segment) ?: "Unknown Action")
        var instruction = segment.miniInstruction?.description
        if (segment.metres > 0) {
            notes.set(DistanceFormatter.format(segment.metres))
        } else {
            notes.set(segment.notes)
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
                if (localResource > 0) {
                    icon.set(ContextCompat.getDrawable(context, localResource))
                } else {
                    icon.set(null)
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
    }
}