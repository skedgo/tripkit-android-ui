package com.skedgo.tripkit.ui.core.module
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMapFragment
import com.skedgo.tripkit.ui.timetables.TimetableMapContributor

import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [LocationStuffModule::class, ServiceDetailsModule::class, TripDetailsModule::class])
interface ServiceStopMapComponent {
  fun inject(fragment: ServiceStopMapFragment)
  fun inject(contributor: TimetableMapContributor)

}
