package com.skedgo.tripkit.ui.utils

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes

object ViewUtils {
    @JvmStatic
    fun setText(view: TextView?, text: CharSequence?) {
        if (view != null) {
            view.text = text
            if (TextUtils.isEmpty(text)) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
            }
        }
    }

    fun setImage(view: ImageView, @DrawableRes res: Int) {
        if (res != 0) {
            view.setImageResource(res)
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }
}