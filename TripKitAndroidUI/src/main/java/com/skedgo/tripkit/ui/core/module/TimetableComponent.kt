package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.provider.TimetableProvider
import dagger.Subcomponent

@Subcomponent(modules = [TimetableModule::class,
  ServiceDetailsModule::class
])
interface TimetableComponent {
  fun inject(view: TimetableProvider)
}