package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.fetchAsync
import io.reactivex.Observable

fun Drawable.tint(color: Int): Drawable {
    val mutate = this.mutate()
    this.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
    return mutate
}

class DrawableUtils {
    companion object {
        fun setDrawable(context: Context, modeInfo: ModeInfo?): Observable<Drawable> {
            val concatUrl = TransportModeUtils.getIconUrlForModeInfo(context.resources, modeInfo)
            var remoteIcon = Observable.empty<Drawable>()
            if (!concatUrl.isNullOrEmpty()) {
                remoteIcon = TripKitUI.getInstance().picasso().fetchAsync(concatUrl).toObservable()
                    .map { bitmap ->
                        BitmapDrawable(context.resources, bitmap)
                    }
            }

            return remoteIcon
        }
    }
}