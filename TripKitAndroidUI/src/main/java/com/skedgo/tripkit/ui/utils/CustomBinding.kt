package com.skedgo.tripkit.ui.utils

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

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
            Glide.with(imageView.context)
                    .load(it)
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