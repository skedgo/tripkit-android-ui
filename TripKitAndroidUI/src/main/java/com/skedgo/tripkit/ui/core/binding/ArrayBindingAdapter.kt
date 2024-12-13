package com.skedgo.tripkit.ui.core.binding

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.views.TripSegmentAlertView

@BindingAdapter("entries", "layout")
fun <T> setEntries(
    viewGroup: ViewGroup,
    entries: List<T>?,
    layoutId: Int
) {
    viewGroup.removeAllViews()
    if (entries != null) {
        val inflater = viewGroup.context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        for (entry in entries) {
            val binding = DataBindingUtil
                .inflate<ViewDataBinding>(inflater, layoutId, viewGroup, true)
            binding.setVariable(BR.viewModel, entry)
        }
    }
}

@BindingAdapter("alerts")
fun setAlertEntries(viewGroup: ViewGroup, alerts: ArrayList<RealtimeAlert>) {
    if (viewGroup is TripSegmentAlertView) {
        viewGroup.setAlerts(alerts)
    }
}
