package com.skedgo.tripkit.ui.core.module;

import com.skedgo.tripkit.ui.dialog.TripKitDateTimePickerDialogFragment;
import dagger.Subcomponent;

@ActivityScope
@Subcomponent(modules = TimePickerModule.class)
public interface TimePickerComponent {
  void inject(TripKitDateTimePickerDialogFragment fragment);
}