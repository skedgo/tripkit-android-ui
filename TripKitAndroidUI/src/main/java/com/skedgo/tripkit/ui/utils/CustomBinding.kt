package com.skedgo.tripkit.ui.utils

import android.content.res.Resources
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

//To databind resource id(int) on image views
@BindingAdapter("android:src")
fun setIcon(view: ImageView, iconResource: Int) {
    try {
        ContextCompat.getDrawable(view.context, iconResource)?.let {
            view.setImageDrawable(it)
        }
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