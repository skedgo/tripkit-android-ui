package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.dialog.TripKitDateTimePickerDialogFragment
import com.skedgo.tripkit.ui.dialog.v2.datetimepicker.TKUIDateTimePickerDialogFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [TimePickerModule::class])
interface TimePickerComponent {
    fun inject(fragment: TripKitDateTimePickerDialogFragment)
    fun inject(fragment: TKUIDateTimePickerDialogFragment)
}