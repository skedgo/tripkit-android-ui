package com.skedgo.tripkit.ui.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

public final class Snackbars {
    private Snackbars() {
    }

    public static void showShortly(@NonNull FragmentActivity activity, @Nullable String message) {
        final View v = activity.findViewById(android.R.id.content);
        Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show();
    }
}