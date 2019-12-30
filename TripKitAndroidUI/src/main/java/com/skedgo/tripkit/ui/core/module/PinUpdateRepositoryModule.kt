package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.tripplanner.PinUpdateRepository
import com.skedgo.tripkit.ui.map.PinUpdateRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class PinUpdateRepositoryModule {
  @Provides
  fun pinUpdateRepository(): PinUpdateRepository = PinUpdateRepositoryImpl()
}
