package com.skedgo.tripkit.ui.core.module
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMapFragment

import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [LocationStuffModule::class, ServiceDetailsModule::class])
interface ServiceStopMapComponent {
  fun inject(fragment: ServiceStopMapFragment)
}
