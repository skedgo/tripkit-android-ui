package com.skedgo.tripkit.ui.core.binding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.skedgo.tripkit.common.model.TransportMode;
import com.skedgo.tripkit.common.util.TransportModeUtils;
import com.skedgo.tripkit.routing.ModeInfo;
import com.skedgo.tripkit.routing.VehicleMode;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.TripKitUI;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

import static com.skedgo.tripkit.common.util.TransportModeUtils.getIconUrlForId;

public final class ImageViewBindingAdapters {
    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, boolean visible) {
        try {
            view.animate().cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("fadeVisible")
    public static void setFadeVisible(final View view, boolean visible) {
        if (view.getTag() == null) {
            view.setTag(true);
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        } else {
            view.animate().cancel();

            if (visible) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(0);
                view.animate().alpha(1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setAlpha(1);
                    }
                });
            } else {
                view.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setAlpha(1);
                        view.setVisibility(View.GONE);
                    }
                });
            }
        }
    }


    @BindingAdapter("android:src")
    public static void setModeInfo(ImageView view, @Nullable ModeInfo modeInfo) {
        loadModeInfo(view, modeInfo);
    }

    @BindingAdapter("app:srcCompat")
    public static void setCompatModeInfo(ImageView view, @Nullable ModeInfo modeInfo) {
        loadModeInfo(view, modeInfo);
    }

    private static void loadModeInfo(ImageView view, @Nullable ModeInfo modeInfo) {
        if (modeInfo == null) {
            TripKitUI.getInstance().picasso()
                .load(R.drawable.ic_public_transport)
                .into(view);
            return;
        }

        final VehicleMode mode = modeInfo.getModeCompat();
        int placeHolder = mode != null ? mode.getIconRes() : R.drawable.ic_public_transport;
        final String url = TransportModeUtils.getIconUrlForModeInfo(view.getResources(), modeInfo);
        TripKitUI.getInstance().picasso()
            .load(url)
            .placeholder(placeHolder)
            .error(placeHolder)
            .into(view);
    }

    @BindingAdapter("modeId")
    public static void bindModeId(ImageView iconView, String modeId) {
        int resId = TransportMode.getLocalIconResId(modeId);
        if (resId != 0) {
            iconView.setImageResource(resId);
        }
    }

    @BindingAdapter("android:src")
    public static void setImageResource(ImageView imageView, int resource) {
        imageView.setImageResource(resource);
    }

    @BindingAdapter("modeIconId")
    public static void bindModeIconId(ImageView view, String modeIconId) {
        final int resId = TransportMode.getLocalIconResId(modeIconId);
        if (resId == 0) {
            if (!TextUtils.isEmpty(modeIconId)) {
                final String url = getIconUrlForId(view.getResources(), modeIconId);
                TripKitUI.getInstance().picasso()
                    .load(url)
                    .placeholder(R.drawable.ic_car_ride_share)
                    .error(R.drawable.ic_car_ride_share)
                    .into(view);
            } else {
                view.setImageResource(R.drawable.ic_car_ride_share);
            }
        }
    }

    @BindingAdapter("app:tint")
    public static void setTint(ImageView view, Integer color) {
        if (color != null) {
            view.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        } else {
            view.clearColorFilter();
        }
    }

}