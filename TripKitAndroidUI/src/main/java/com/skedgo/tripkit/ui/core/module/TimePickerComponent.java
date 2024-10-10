package com.skedgo.tripkit.ui.core.module;

import com.skedgo.tripkit.ui.dialog.TripKitDateTimePickerDialogFragment;
import com.skedgo.tripkit.ui.dialog.v2.datetimepicker.TKUIDateTimePickerDialogFragment;

import dagger.Subcomponent;

@ActivityScope
@Subcomponent(modules = TimePickerModule.class)
public interface TimePickerComponent {
    void inject(TripKitDateTimePickerDialogFragment fragment);
    void inject(TKUIDateTimePickerDialogFragment fragment);
}