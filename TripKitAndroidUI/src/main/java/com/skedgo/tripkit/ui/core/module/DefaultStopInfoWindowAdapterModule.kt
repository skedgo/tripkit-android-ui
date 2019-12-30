package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.map.DefaultStopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import dagger.Module
import dagger.Provides

@Module
class DefaultStopInfoWindowAdapterModule {

  @Provides
  fun stopInfoWindowAdapter(
      defaultInfoWindowAdapter: DefaultStopInfoWindowAdapter): StopInfoWindowAdapter {
    return defaultInfoWindowAdapter
  }

}