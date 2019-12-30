package com.skedgo.tripkit.ui.utils

import android.graphics.drawable.Drawable

fun Drawable.tint(color: Int): Drawable {
    val mutate = this.mutate()
    this.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
    return mutate
}