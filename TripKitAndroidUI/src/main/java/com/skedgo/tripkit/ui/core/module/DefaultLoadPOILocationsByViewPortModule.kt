package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.map.DefaultLoadPOILocationsByViewPort
import com.skedgo.tripkit.ui.map.LoadPOILocationsByViewPort
import dagger.Module
import dagger.Provides

@Module
class DefaultLoadPOILocationsByViewPortModule {

  @Provides
  fun loadPOILocationsByViewPort(
      defaultLoadPOILocationsByViewPort: DefaultLoadPOILocationsByViewPort)
      : LoadPOILocationsByViewPort = defaultLoadPOILocationsByViewPort

}