package com.skedgo.tripkit.ui.utils

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.RadioButton
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.doOnAttach
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.databinding.BindingAdapter
import com.skedgo.tripkit.ui.R

private fun configuredTint(fallback: Int): Int = DynamicAppColor.getAppColors()?.tintColor?.let {
    Color.rgb(it.red, it.green, it.blue)
} ?: DynamicAppColor.getSystemColors()?.tintColor ?: fallback

private fun surfaceColor(view: View?): Int? {
    if (view == null) return null
    val parentColor = surfaceColor(view.parent as? View)
        ?: ContextCompat.getColor(view.context, R.color.white)
    val color = when (view) {
        is CardView -> view.cardBackgroundColor.defaultColor
        else -> when (val background = view.background) {
            is ColorDrawable -> background.color
            is GradientDrawable -> background.color?.defaultColor
            else -> null
        }
    } ?: return parentColor
    return ColorUtils.compositeColors(color, parentColor)
}

private fun readableForeground(view: View, fallback: Int): Int {
    val brand = configuredTint(fallback)
    val night = view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    var foreground = brand
    if (night) {
        val surface = surfaceColor(view.parent as? View)
            ?: ContextCompat.getColor(view.context, R.color.white)
        val preferred = ContextCompat.getColor(view.context, R.color.black)
        for (step in 0..20) {
            foreground = ColorUtils.blendARGB(brand, preferred, step / 20f)
            if (ColorUtils.calculateContrast(foreground, surface) >= 4.5) break
        }
    }
    return foreground
}

/** Scoped return-picker switch colors; keeps enabled thumbs opaque on the containing card. */
@BindingAdapter("oneWaySwitchTint")
fun bindOneWaySwitchTint(view: androidx.appcompat.widget.SwitchCompat, fallback: Int) {
    fun update() {
        val surface = surfaceColor(view.parent as? View)
            ?: ContextCompat.getColor(view.context, R.color.white)
        val checked = readableForeground(view, fallback)
        val unchecked = ContextCompat.getColor(view.context, R.color.labelSecondary)
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(),
        )
        val checkedTrack = ColorUtils.blendARGB(surface, checked, 0.15f)
        val uncheckedTrack = ColorUtils.blendARGB(surface, unchecked, 0.15f)
        view.thumbTintList = ColorStateList(states, intArrayOf(
            ColorUtils.blendARGB(surface, checked, 0.38f),
            ColorUtils.blendARGB(surface, unchecked, 0.38f),
            checked, unchecked,
        ))
        view.trackTintList = ColorStateList(states, intArrayOf(
            ColorUtils.blendARGB(surface, checkedTrack, 0.38f),
            ColorUtils.blendARGB(surface, uncheckedTrack, 0.38f),
            checkedTrack, uncheckedTrack,
        ))
    }
    update()
    view.doOnAttach { update() }
}

/** Keeps configured action branding in light mode and gives dark-surface labels readable contrast. */
@BindingAdapter("readableAppTextTint")
fun bindReadableAppTextTint(view: TextView, fallback: Int) {
    view.setTextColor(readableForeground(view, fallback))
}

/** Shared label/icon foreground for the outlined trip-result actions and their interaction states. */
@BindingAdapter("readableAppActionTint")
fun bindReadableAppActionTint(view: View, fallback: Int) {
    val foreground = readableForeground(view, fallback)
    val night = view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    val interactive = if (night) ContextCompat.getColor(view.context, R.color.black) else foreground
    val disabledAlpha = android.util.TypedValue().let { value ->
        if (view.context.theme.resolveAttribute(android.R.attr.disabledAlpha, value, true)) value.float else 0.38f
    }
    val colors = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf(android.R.attr.state_focused),
            intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
        intArrayOf(interactive, interactive, ColorUtils.setAlphaComponent(foreground, (255 * disabledAlpha).toInt()), foreground)
    )
    when (view) {
        is TextView -> view.setTextColor(colors)
        is ImageView -> view.imageTintList = colors
    }
}

/** Foregrounds for the date picker's branded checked surface and lightly tinted unchecked surface. */
@BindingAdapter("readablePickerRadioText")
fun bindReadablePickerRadioText(view: RadioButton, fallback: Int) {
    val surface = surfaceColor(view.parent as? View)
        ?: ContextCompat.getColor(view.context, R.color.white)
    val checkedSurface = ColorUtils.compositeColors(configuredTint(fallback), surface)
    val checked = if (ColorUtils.calculateContrast(Color.WHITE, checkedSurface) >=
        ColorUtils.calculateContrast(Color.BLACK, checkedSurface)) Color.WHITE else Color.BLACK
    view.setTextColor(ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(checked, ContextCompat.getColor(view.context, R.color.black))
    ))
}

/** Selector fills distinguish filter selection without fading enabled mode icons below readable contrast. */
@BindingAdapter("readableTransportChecked")
fun bindReadableTransportChecked(view: com.skedgo.tripkit.ui.views.CheckableImageButton, checked: Boolean?) {
    view.setChecked(checked == true)
    view.alpha = 1f
}

/** Retains each action's own color (including destructive red) while making its text readable. */
@BindingAdapter("readableActionTextColor")
fun bindReadableActionTextColor(view: TextView, color: Int) {
    fun update() {
        val surface = surfaceColor(view.parent as? View)
            ?: ContextCompat.getColor(view.context, R.color.white)
        val preferred = if (ColorUtils.calculateContrast(Color.WHITE, surface) >=
            ColorUtils.calculateContrast(Color.BLACK, surface)) Color.WHITE else Color.BLACK
        var foreground = color
        for (step in 0..20) {
            foreground = ColorUtils.blendARGB(color, preferred, step / 20f)
            if (ColorUtils.calculateContrast(foreground, surface) >= 4.5) break
        }
        view.setTextColor(foreground)
    }
    update()
    // Recycler rows can bind before attachment, when their containing surface is not available yet.
    view.doOnAttach { update() }
}

/** Keeps white badge text where readable and uses black on lighter opaque badge fills. */
@BindingAdapter("readableBadgeTextColor")
fun bindReadableBadgeTextColor(view: TextView, background: Int) {
    view.setTextColor(if (ColorUtils.calculateContrast(Color.WHITE, background) >= 4.5) Color.WHITE else Color.BLACK)
}
