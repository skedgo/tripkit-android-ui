package com.skedgo.tripkit.ui.core

import android.view.View
import androidx.databinding.BindingConversion

object Converters {

    /**
     * Binds a boolean into [View.setVisibility].
     * Sample: `android:visibility="@{viewModel.isBusy}"`
     * `isBusy` can be an [ObservableBoolean].
     */
    @BindingConversion
    @JvmStatic
    fun convertBooleanToViewVisibility(value: Boolean): Int {
        return if (value) View.VISIBLE else View.GONE
    }
}
