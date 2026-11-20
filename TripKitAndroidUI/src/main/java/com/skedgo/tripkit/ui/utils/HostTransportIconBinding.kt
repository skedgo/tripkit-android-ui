package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.hostThemeColor
import com.skedgo.tripkit.ui.tripresult.TripSegmentItemViewModel
import com.skedgo.tripkit.ui.tripresults.TripSegmentViewModel

/** Correct application-context tinting at the View boundary, without recoloring brands/alerts. */
internal fun hostTransportIcon(
    context: Context,
    drawable: Drawable,
    preserveColor: Boolean,
    useServiceColors: Boolean = context.resources.getBoolean(R.bool.trip_kit_use_service_color),
    hasAlertOverlay: Boolean = false
): Drawable {
    if (preserveColor || useServiceColors) return drawable
    val copy = drawable.constantState?.newDrawable(context.resources)?.mutate() ?: return drawable
    copy.clearColorFilter()
    val transport = if (hasAlertOverlay && copy is LayerDrawable) copy.getDrawable(0) else copy
    transport.setColorFilter(
        context.hostThemeColor(com.google.android.material.R.attr.colorOnSurface, R.color.black),
        PorterDuff.Mode.SRC_IN
    )
    return copy
}

@BindingAdapter("hostResultIcon", "hostResultIconModel")
fun bindHostResultIcon(view: ImageView, drawable: Drawable?, model: TripSegmentViewModel?) {
    val night = view.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    view.setImageDrawable(if (drawable == null || model == null || model.iconNightMode == null || model.iconNightMode == night) drawable else
        hostTransportIcon(view.context, drawable, model.preservesTransportIconColor,
            hasAlertOverlay = model.hasTransportAlertOverlay))
}

@BindingAdapter("hostDetailIcon", "hostDetailIconModel")
fun bindHostDetailIcon(view: ImageView, drawable: Drawable?, model: TripSegmentItemViewModel?) {
    val preserve = model?.iconPreservesColor == true || model?.showBackgroundCircle?.value == true ||
        model?.tripSegment?.modeInfo?.remoteIconIsBranding == true
    val night = view.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    view.setImageDrawable(if (drawable == null || model == null || model.iconNightMode == null || model.iconNightMode == night) drawable else
        hostTransportIcon(view.context, drawable, preserve))
}

/** Preview defaults are dark glyphs; resolve the foreground from the displaying host. */
@BindingAdapter("hostPreviewIcon", "hostPreviewIconModel")
fun bindHostPreviewIcon(view: ImageView, drawable: Drawable?, model: com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel?) {
    view.setImageDrawable(drawable?.let {
        hostTransportIcon(view.context, it,
            preserveColor = model?.segment?.modeInfo?.remoteIconIsBranding == true,
            useServiceColors = false)
    })
}
