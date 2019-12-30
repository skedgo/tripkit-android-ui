package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.trip.details.viewmodel.ITimePickerViewModel
import com.skedgo.tripkit.ui.trip.options.InterCityTimePickerViewModel
import com.squareup.otto.Bus
import dagger.Module
import dagger.Provides
import java.util.*

@Module
class TimePickerModule {
  @Provides
  fun timePickerViewModel(
      context: Context,
      bus: Bus,
      getNow: GetNow
  ): ITimePickerViewModel = InterCityTimePickerViewModel(
      context,
      bus,
      getNow,
      TimeZone.getDefault().id
  )
}
