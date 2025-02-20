package com.skedgo.tripkit.ui.core.binding

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.PorterDuff.Mode.SRC_IN
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.skedgo.tripkit.common.model.TransportMode.Companion.getLocalIconResId
import com.skedgo.tripkit.common.util.TransportModeUtils.getIconUrlForId
import com.skedgo.tripkit.common.util.TransportModeUtils.getIconUrlForModeInfo
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.GlideApp
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import timber.log.Timber

object ImageViewBindingAdapters {
    @JvmStatic
    @BindingAdapter("android:visibility")
    fun setVisibility(view: View, visible: Boolean) {
        try {
            view.animate().cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @JvmStatic
    @BindingAdapter("fadeVisible")
    fun setFadeVisible(view: View, visible: Boolean) {
        if (view.tag == null) {
            view.tag = true
            view.visibility = if (visible) View.VISIBLE else View.GONE
        } else {
            view.animate().cancel()

            if (visible) {
                view.visibility = View.VISIBLE
                view.alpha = 0f
                view.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.alpha = 1f
                    }
                })
            } else {
                view.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.alpha = 1f
                        view.visibility = View.GONE
                    }
                })
            }
        }
    }


    @JvmStatic
    @BindingAdapter("android:src")
    fun setModeInfo(view: ImageView, modeInfo: ModeInfo?) {
        loadModeInfo(view, modeInfo)
    }

    @JvmStatic
    @BindingAdapter("app:srcCompat")
    fun setCompatModeInfo(view: ImageView, modeInfo: ModeInfo?) {
        loadModeInfo(view, modeInfo)
    }

    private fun loadModeInfo(view: ImageView, modeInfo: ModeInfo?) {
        if (modeInfo == null) {
            TripKitUI.getInstance().picasso()
                .load(R.drawable.ic_public_transport)
                .into(view)
            return
        }

        val mode = modeInfo.modeCompat
        val placeHolder = mode?.iconRes ?: R.drawable.ic_public_transport
        val url = getIconUrlForModeInfo(view.resources, modeInfo)
        TripKitUI.getInstance().picasso()
            .load(url)
            .placeholder(placeHolder)
            .error(placeHolder)
            .into(view)
    }

    @JvmStatic
    @BindingAdapter("modeId")
    fun bindModeId(iconView: ImageView, modeId: String?) {
        val resId = getLocalIconResId(modeId)
        if (resId != 0) {
            iconView.setImageResource(resId)
        }
    }

    @JvmStatic
    @BindingAdapter("android:src")
    fun setImageResource(imageView: ImageView, resource: Int) {
        imageView.setImageResource(resource)
    }

    @JvmStatic
    @BindingAdapter("modeIconId")
    fun bindModeIconId(view: ImageView, modeIconId: String?) {
        Timber.tag("ImageViewBindingAdapters: modeIconId").i("$modeIconId")
        val resId = getLocalIconResId(modeIconId)
        if (resId == 0) {
            if (!TextUtils.isEmpty(modeIconId)) {
                val url = getIconUrlForId(view.resources, modeIconId)
                Timber.tag("ImageViewBindingAdapters: modeIconId").i("loading icon from: $url")
                GlideApp.with(view.context)
                    .load(url)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.ic_car_ride_share)
                    .error(R.drawable.ic_car_ride_share)
                    .into(view)
            } else {
                Timber.tag("ImageViewBindingAdapters: modeIconId").i("loading icon from local")
                view.setImageResource(R.drawable.ic_car_ride_share)
            }
        }
    }

    @JvmStatic
    @BindingAdapter("app:tint")
    fun setTint(view: ImageView, color: Int?) {
        if (color != null) {
            view.setColorFilter(color, SRC_IN)
        } else {
            view.clearColorFilter()
        }
    }
}