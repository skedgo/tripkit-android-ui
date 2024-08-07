package com.skedgo.tripkit.ui.utils;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public final class ViewUtils {
    private ViewUtils() {
    }

    public static void setText(TextView view, CharSequence text) {
        if (view != null) {
            view.setText(text);
            if (TextUtils.isEmpty(text)) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
            }
        }
    }

    public static void setImage(@NonNull ImageView view, @DrawableRes int res) {
        if (res != 0) {
            view.setImageResource(res);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}