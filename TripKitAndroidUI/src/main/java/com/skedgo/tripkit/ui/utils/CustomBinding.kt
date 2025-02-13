package com.skedgo.tripkit.ui.utils

import android.content.res.ColorStateList
import android.content.res.Resources.NotFoundException
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
        view.setBackgroundColor(it)
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

@BindingAdapter("appTint")
fun setImageViewAppTint(imageView: ImageView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    color?.let {
        imageView.setColorFilter(it, PorterDuff.Mode.SRC_IN) // Apply tint only to fill
    }
}

@BindingAdapter("appTint")
fun setTextViewAppTint(textView: TextView, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.tintColor?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.tintColor ?: default

    color?.let {
        textView.setTextColor(it)
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

@BindingAdapter("appBackground")
fun setLayoutAppBackgroundTint(view: View, @ColorInt default: Int?) {
    val color = DynamicAppColor.getAppColors()?.barBackground?.let {
        Color.rgb(it.red, it.green, it.blue)
    } ?: DynamicAppColor.getSystemColors()?.barBackground ?: default

    color?.let {
        view.setBackgroundColor(it)
    }
}
