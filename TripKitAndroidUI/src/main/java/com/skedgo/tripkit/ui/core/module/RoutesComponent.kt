package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.tripresult.TripResultListMapContributor
import com.skedgo.tripkit.ui.tripresults.TripResultListFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [RoutesModule::class, LocationStuffModule::class, CameraPositionDataModule::class, TripDetailsModule::class])
interface RoutesComponent {
    fun inject(fragment: TripResultListFragment)
    fun inject(contributor: TripResultListMapContributor)
}