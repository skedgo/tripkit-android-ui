package com.skedgo.tripkit.ui.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable

/** Unlike BitmapDrawable.toBitmap's fast path, drawing preserves tint and color filters. */
internal fun Drawable.tripDrawableBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888
    )
    val previousBounds = Rect(bounds)
    try {
        setBounds(0, 0, bitmap.width, bitmap.height)
        draw(Canvas(bitmap))
    } finally {
        bounds = previousBounds
    }
    return bitmap
}
