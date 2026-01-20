package com.skedgo.tripkit.ui.core

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat

/**
 * Global typography configuration for TripKit Android UI (Views-based).
 *
 * Supports both:
 * - a font resource id (preferred, e.g. `R.font.roboto`)
 * - a font family name fallback (e.g. "sans-serif")
 *
 * Applied automatically in TripKit base fragments/dialogs.
 */
object TripKitUITypography {
    @Volatile
    private var fontFamilyName: String? = null

    @Volatile
    @FontRes
    private var fontResId: Int? = null

    @JvmStatic
    fun setFontFamilyName(fontFamilyName: String?) {
        this.fontFamilyName = fontFamilyName
        this.fontResId = null
    }

    @JvmStatic
    fun setFont(@FontRes fontResId: Int?) {
        this.fontResId = fontResId
        this.fontFamilyName = null
    }

    @JvmStatic
    fun getFontFamilyName(): String? = fontFamilyName

    @JvmStatic
    fun getFontResId(): Int? = fontResId

    fun applyTo(root: View) {
        val resId = fontResId
        val family = fontFamilyName
        if (resId == null && family == null) return
        applyRecursive(root, resId, family)
    }

    private fun applyRecursive(view: View, @FontRes resId: Int?, family: String?) {
        if (view is TextView) {
            val currentStyle = view.typeface?.style ?: Typeface.NORMAL
            val typeface = when {
                resId != null -> runCatching { ResourcesCompat.getFont(view.context, resId) }.getOrNull()
                family != null -> Typeface.create(family, Typeface.NORMAL)
                else -> null
            }
            if (typeface != null) {
                view.typeface = Typeface.create(typeface, currentStyle)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyRecursive(view.getChildAt(i), resId, family)
            }
        }
    }
}

