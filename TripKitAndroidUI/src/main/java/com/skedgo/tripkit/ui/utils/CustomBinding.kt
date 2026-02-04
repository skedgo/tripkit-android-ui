package com.skedgo.tripkit.ui.utils

import android.content.res.ColorStateList
import android.content.res.Resources.NotFoundException
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import com.skedgo.tripkit.common.model.alert.AlertSeverity
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.GlideApp
import com.skedgo.tripkit.ui.R

//To databind resource id(int) on image views
@BindingAdapter("android:src")
fun setIcon(view: ImageView, iconResource: Int?) {
    try {
        iconResource?.let {
            ContextCompat.getDrawable(view.context, iconResource)?.let {
                view.setImageDrawable(it)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@BindingAdapter("android:src")
fun setIconDrawable(view: ImageView, drawable: Drawable?) {
    try {
        view.setImageDrawable(drawable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@BindingAdapter("drawableSource")
fun setIcon(view: ImageView, iconResource: Drawable?) {
    try {
        view.setImageDrawable(iconResource)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

//To avoid button click spam
@BindingAdapter("clickWithDebounce")
fun setDebounceClickListener(view: View, onClickListener: View.OnClickListener) {
    val debounceTime = 800L
    var lastClickTime: Long = 0

    val clickWithDebounce: (view: View) -> Unit = {
        if (SystemClock.elapsedRealtime() - lastClickTime >= debounceTime) {
            onClickListener.onClick(it)
        }
        lastClickTime = SystemClock.elapsedRealtime()
    }

    view.setOnClickListener(clickWithDebounce)
}

@BindingAdapter("customImageTint")
fun ImageView.setImageTint(@ColorInt color: Int) {
    if (color != 0) {
        setColorFilter(color)
    } else {
        clearColorFilter()
    }
}

@BindingAdapter("visibilityInv")
fun setVisibilityInv(view: View, visible: Boolean?) {
    view.visibility = if (visible == true) {
        View.VISIBLE
    } else {
        View.INVISIBLE
    }
}

@BindingAdapter("android:text")
fun setListValuesToText(textView: TextView, values: List<String>?) {
    if (values != null && values.isNotEmpty()) {
        val sb = StringBuilder()
        values.forEachIndexed { index, s ->
            sb.append(s)
            if (index < values.size - 1) {
                sb.append(System.lineSeparator())
            }
        }
        textView.text = sb.toString()
    }
}

@BindingAdapter("imageUrl")
fun setImageFromUrl(imageView: ImageView, source: String?) {
    source?.let {
        if (it.isNotBlank()) {
            val glideUrl = GlideUrl(
                it,
                LazyHeaders.Builder()
                    .addHeader("X-TripGo-Key", "c61e6c3e4b9f1af4797f9a0974022b1f")
                    .addHeader("Accept", "image/jpeg")
                    .build()
            )
            GlideApp.with(imageView.context)
                .load(glideUrl)
                .into(imageView)
        }
    }
}

@BindingAdapter("sourceUrl", "placeholder", "tripGoKey", "tripGoClientId", "userToken")
fun setImageFromUrlWithPlaceholder(
    imageView: ImageView,
    source: String?,
    placeholder: Int,
    tripGoKey: String,
    tripGoClientId: String?,
    userToken: String?
) {
    source?.let {
        if (it.isNotBlank()) {
            val headersBuilder = LazyHeaders.Builder()
                .addHeader("X-TripGo-Key", tripGoKey)
                .addHeader("Accept", "image/*")
            tripGoClientId?.let {
                headersBuilder.addHeader("X-TripGo-Client-Id", it)
            }
            userToken?.let {
                headersBuilder.addHeader("userToken", userToken)
            }
            val glideUrl = GlideUrl(it, headersBuilder.build())
            GlideApp.with(imageView.context)
                .load(glideUrl)
                .apply(
                    RequestOptions()
                        .placeholder(placeholder)
                )
                .into(imageView)
        }
    }
}

@BindingAdapter("mirrorImage")
fun setMirrorImage(imageView: ImageView, isMirrored: Boolean) {
    if (isMirrored) {
        imageView.scaleX = -1.0f
    }
}

@BindingAdapter("addRtlSupport")
fun setAddRtlSupport(view: View, addRtlSupport: Boolean) {
    if (addRtlSupport) {
        val isRightToLeft = view.context.resources.getBoolean(R.bool.is_right_to_left)
        view.textDirection = if (isRightToLeft) {
            View.TEXT_DIRECTION_RTL
        } else {
            View.TEXT_DIRECTION_LOCALE
        }

        if (view is ImageView) {
            setMirrorImage(view, isRightToLeft)
        }
    }
}

@BindingAdapter("backgroundDrawable")
fun setBackgroundDrawable(view: View, drawable: Drawable?) {
    drawable?.let {
        view.background = it
    }
}

@BindingAdapter("accessibilityViewFocus")
fun focusViewForAccessibility(view: View, focus: Boolean) {
    if (focus) {
        view.postDelayed({
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }, 500)
    }
}

@BindingAdapter("app:cardBackgroundColor")
fun setCardBackgroundColor(view: CardView, color: Int?) {
    color?.let {
        // Use CardView API to preserve corner radius shape
        view.setCardBackgroundColor(it)
    }
}

@BindingAdapter("android:textColor")
fun setTextViewTextColor(view: TextView, color: Int?) {
    color?.let {
        view.setTextColor(it)
    }
}

@BindingAdapter("android:src")
fun setAlertIcon(view: ImageView, @AlertSeverity severity: String) {
    try {
        val iconResource = if (severity == RealtimeAlert.SEVERITY_ALERT) {
            R.drawable.ic_alert_red_overlay
        } else {
            R.drawable.ic_alert_yellow_overlay
        }
        ContextCompat.getDrawable(view.context, iconResource)?.let {
            view.setImageDrawable(it)
        }
    } catch (e: NotFoundException) {
        e.printStackTrace()
    }
}

@BindingAdapter("android:backgroundTint")
fun setBackgroundTint(view: View, color: Int?) {
    view.backgroundTintList = color?.let {
        ContextCompat.getColorStateList(view.context, it)
    }
}

@BindingAdapter("visibilityByHeight")
fun setLayoutHeight(view: View, show: Boolean) {
    val layoutParams = view.layoutParams
    layoutParams.height = if (show) {
        ViewGroup.LayoutParams.WRAP_CONTENT
    } else {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0f, view.resources.displayMetrics)
            .toInt()
    }
    view.layoutParams = layoutParams
}

@BindingAdapter("imageViewSize")
fun setImageViewSize(view: ImageView, size: Float) {
    val layoutParams = view.layoutParams
    layoutParams.height =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, view.resources.displayMetrics)
            .toInt()
    layoutParams.width =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, view.resources.displayMetrics)
            .toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:background")
fun setBackground(view: View, drawable: StateListDrawable?) {
    try {
        drawable?.let { view.background = it }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@BindingAdapter("clearTag")
fun clearTag(view: View, clear: Boolean) {
    if (clear) {
        view.tag = ""
    }
}

@BindingAdapter("dynamicHeight")
fun setDynamicHeight(view: View, heightInDp: Int) {
    val params = view.layoutParams
    val scale = view.context.resources.displayMetrics.density // Convert dp to pixels
    params.height = if (heightInDp > 0) (heightInDp * scale).toInt() else 0
    view.layoutParams = params
}

@BindingAdapter("android:drawableTint")
fun TextView.setDrawableTint(color: Int) {
    compoundDrawables.filterNotNull().forEach {
        if (color != 0) {
            it.colorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(context, color),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            it.clearColorFilter()
        }
    }
}

/**
 * =================== Dynamic color binding using colors from [DynamicAppColor] ===================
 */

@BindingAdapter("appTint")
fun setButtonAppTint(button: Button, @ColorInt default: Int) {
    DynamicAppColor.getAppColors()?.tintColor?.let {
        val color = Color.rgb(it.red, it.green, it.blue)
        button.backgroundTintList = ColorStateList.valueOf(color)
    } ?: run {
        DynamicAppColor.getSystemColors()?.tintColor?.let {
            button.backgroundTintList = ColorStateList.valueOf(it)
        } ?: run {
            button.backgroundTintList = ColorStateList.valueOf(default)
        }
    }
}

@BindingAdapter("appTintWithState")
fun setButtonStateBackground(button: Button, @ColorInt default: Int?) {
    val enabledColor = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    val disabledColor = enabledColor?.let {
        ColorUtils.setAlphaComponent(it, (0.3 * 255).toInt()) // 30% opacity for disabled state
    } ?: Color.GRAY // Fallback to gray if null

    val colorStateList = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled), // Disabled state
            intArrayOf(android.R.attr.state_enabled) // Enabled state
        ),
        intArrayOf(
            disabledColor, // Color when disabled
            enabledColor ?: Color.TRANSPARENT // Color when enabled
        )
    )

    // Create a GradientDrawable to ensure background supports tinting
    val backgroundDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius =
            button.resources.getDimension(R.dimen.button_radius_small) // Adjust as needed
        setColor(enabledColor ?: Color.TRANSPARENT) // Set initial color
    }

    // Set the background explicitly so tinting works
    button.background = backgroundDrawable
    button.backgroundTintList = colorStateList

    // Ensure state change is reflected
    button.invalidate()
    button.refreshDrawableState()
}

@BindingAdapter("appTint")
fun setImageViewAppTint(imageView: ImageView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default.takeIf { default != 0 }

    color?.let {
        imageView.setColorFilter(it, PorterDuff.Mode.SRC_IN)
    }
}

@BindingAdapter("appTextTint")
fun setTextViewAppTint(textView: TextView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    color?.let {
        textView.setTextColor(it)
    }
}

@BindingAdapter("appLayoutTint")
fun setLayoutAppTint(view: View, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.barBackground ?: default

    color?.let {
        view.backgroundTintList = ColorStateList.valueOf(it)
    }
}

@BindingAdapter("appSwitchTint")
fun setSwitchCheckedTint(switch: SwitchCompat, @ColorInt default: Int?) {
    val checkedColor = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    val uncheckedColor = checkedColor?.let {
        ColorUtils.setAlphaComponent(it, (0.4 * 255).toInt()) // 40% opacity for unchecked state
    } ?: Color.GRAY // Fallback to gray if everything is null

    val thumbStates = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked), // Checked state
            intArrayOf() // Default (unchecked) state
        ),
        intArrayOf(
            checkedColor ?: Color.TRANSPARENT, // Thumb color when checked
            uncheckedColor // Thumb color when unchecked
        )
    )

    val trackStates = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked), // Checked state
            intArrayOf() // Default (unchecked) state
        ),
        intArrayOf(
            ColorUtils.setAlphaComponent(
                checkedColor ?: Color.TRANSPARENT,
                (0.6 * 255).toInt()
            ), // Track when checked
            ColorUtils.setAlphaComponent(
                uncheckedColor,
                (0.3 * 255).toInt()
            ) // Track when unchecked
        )
    )

    switch.thumbTintList = thumbStates
    switch.trackTintList = trackStates
}

@BindingAdapter("appProgressTint")
fun setProgressBarTint(progressBar: ProgressBar, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    color?.let {
        val colorStateList = ColorStateList.valueOf(it)
        progressBar.indeterminateDrawable?.setTintList(colorStateList) // For indeterminate mode
        progressBar.progressDrawable?.setTintList(colorStateList) // For determinate mode
    }
}

@BindingAdapter("appCalendarSelectedTint")
fun setCalendarViewSelectedTint(calendarView: CalendarView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    color?.let {
        try {
            // Change the selected date text color (requires reflection)
            val field = CalendarView::class.java.getDeclaredField("mDaySelectorPaint")
            field.isAccessible = true
            val paint = field.get(calendarView) as Paint
            paint.color = it
            calendarView.invalidate() // Refresh the UI
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set the selected date background indicator (vertical bar)
        try {
            val field = CalendarView::class.java.getDeclaredField("mSelectedDateVerticalBar")
            field.isAccessible = true
            val drawable =
                ContextCompat.getDrawable(calendarView.context, field.getInt(calendarView))
            drawable?.setTint(it)
            field.set(calendarView, drawable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@BindingAdapter("appTextTint")
fun setButtonTextAppTint(button: Button, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    color?.let {
        button.setTextColor(it)
    }
}

@BindingAdapter("appBackgroundTintAlpha")
fun setTextViewBackgroundTintAlpha(textView: TextView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.argb((0.2 * 255).toInt(), it.red, it.green, it.blue) // Apply 20% alpha
    } ?: DynamicAppColor.getSystemColors()?.tintColor?.let {
        Color.argb((0.2 * 255).toInt(), Color.red(it), Color.green(it), Color.blue(it))
    } ?: default?.let {
        Color.argb((0.2 * 255).toInt(), Color.red(it), Color.green(it), Color.blue(it))
    }

    color?.let {
        textView.backgroundTintList = ColorStateList.valueOf(it)
    }
}

@BindingAdapter("appBackgroundTint")
fun setTextViewBackgroundTint(textView: TextView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    color?.let {
        textView.setBackgroundColor(it)
    }
}

@BindingAdapter("appCheckedTint")
fun setRadioButtonCheckedTint(radioButton: RadioButton, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default.takeIf { default != 0 }

    color?.let {
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), // Checked state
                intArrayOf(-android.R.attr.state_checked) // Unchecked state
            ),
            intArrayOf(
                it, // Checked color
                ColorUtils.setAlphaComponent(
                    it,
                    (0.2 * 255).toInt()
                ) // Unchecked color (60% opacity)
            )
        )

        // Set button tint
        ViewCompat.setBackgroundTintList(radioButton, colorStateList)

        // Apply the color manually to the compound button drawable
        radioButton.compoundDrawablesRelative.forEach { drawable ->
            drawable?.setTintList(colorStateList)
        }

        // Force UI refresh
        radioButton.invalidate()
    }
}

@BindingAdapter("appForeground")
fun setTextViewAppForegroundTint(textView: TextView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.barForeground?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.barForeground ?: default

    color?.let {
        textView.setTextColor(it)
    }
}

@BindingAdapter("appForeground")
fun setImageViewAppForegroundTint(imageView: ImageView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.barForeground?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.barForeground ?: default

    color?.let {
        imageView.setColorFilter(it, PorterDuff.Mode.SRC_IN) // Apply tint only to fill
    }
}

@BindingAdapter("appBackground")
fun setLayoutAppBackgroundTint(view: View, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.barBackground?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.barBackground ?: default

    color?.let {
        view.setBackgroundColor(it)
    }
}

@BindingAdapter(value = ["appBackgroundDrawable", "appBackgroundColor"], requireAll = false)
fun setLayoutAppBackgroundAndTint(
    view: View,
    drawable: Drawable?,
    @ColorInt default: Int?
) {
    // pick color: app → system → default (if any)
    val color = DynamicAppColor.getAppColors()?.barBackground?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.barBackground ?: default

    if (drawable != null) {
        val d = DrawableCompat.wrap(drawable.mutate())
        color?.let {
            DrawableCompat.setTint(d, it)
            DrawableCompat.setTintMode(d, PorterDuff.Mode.SRC_IN)
        }
        ViewCompat.setBackground(view, d)
    } else {
        // fallback: no drawable provided, just set a solid color if we have one
        color?.let { view.setBackgroundColor(it) }
    }
}


@BindingAdapter("isDisabled", "originalColor", requireAll = false)
fun setViewIsDisabled(view: View, disabled: Boolean, originalColor: Int?) {
    val disabledColor = ContextCompat.getColor(view.context, R.color.light_grey_3)

    when (view) {
        is ImageView -> {
            if (disabled) {
                view.setColorFilter(disabledColor)
            } else {
                view.clearColorFilter()
            }
        }

        is TextView -> {
            if (disabled) {
                view.setTextColor(disabledColor)
            } else if (originalColor != null) {
                view.setTextColor(originalColor)
            }
        }

        else -> Unit
    }
}