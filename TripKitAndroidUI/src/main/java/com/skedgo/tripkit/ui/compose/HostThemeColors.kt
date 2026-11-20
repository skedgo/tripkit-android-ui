package com.skedgo.tripkit.ui.compose

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

/** Resolve the themed host context first, retaining named SDK resources for legacy hosts. */
@ColorInt
internal fun Context.hostThemeColor(@AttrRes attribute: Int, @ColorRes fallback: Int): Int {
    val values = obtainStyledAttributes(intArrayOf(attribute))
    return try {
        if (values.hasValue(0)) values.getColor(0, ContextCompat.getColor(this, fallback))
        else ContextCompat.getColor(this, fallback)
    } finally {
        values.recycle()
    }
}
