package com.skedgo.tripkit.ui.core;

import android.view.View;

import androidx.databinding.BindingConversion;
import androidx.databinding.ObservableBoolean;

public final class Converters {
    private Converters() {
    }

    /**
     * Binds a boolean into {@link View#setVisibility(int)}.
     * Sample: `android:visibility="@{viewModel.isBusy}`
     * `isBusy` can be an {@link ObservableBoolean}.
     */
    @BindingConversion
    public static int convertBooleanToViewVisibility(boolean value) {
        return value ? View.VISIBLE : View.GONE;
    }
}
