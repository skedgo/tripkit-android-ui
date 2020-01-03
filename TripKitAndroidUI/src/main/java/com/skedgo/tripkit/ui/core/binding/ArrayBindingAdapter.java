package com.skedgo.tripkit.ui.core.binding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import com.skedgo.tripkit.common.model.RealtimeAlert;
import com.skedgo.tripkit.ui.BR;
import com.skedgo.tripkit.ui.views.TripSegmentAlertView;

import java.util.ArrayList;
import java.util.List;

public final class ArrayBindingAdapter {
    @BindingAdapter({"entries", "layout"})
    public static <T> void setEntries(ViewGroup viewGroup,
                                      List<T> entries, int layoutId) {
        viewGroup.removeAllViews();
        if (entries != null) {
            LayoutInflater inflater = (LayoutInflater)
                    viewGroup.getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (int i = 0; i < entries.size(); i++) {
                T entry = entries.get(i);
                ViewDataBinding binding = DataBindingUtil
                        .inflate(inflater, layoutId, viewGroup, true);
                binding.setVariable(BR.viewModel, entry);
            }
        }
    }

    @BindingAdapter({"alerts"})
    public static void setAlertEntries(ViewGroup viewGroup, ArrayList<RealtimeAlert> alerts) {
        if (viewGroup instanceof TripSegmentAlertView) {
            TripSegmentAlertView view = (TripSegmentAlertView) viewGroup;
            view.setAlerts(alerts);
        }
    }
}
