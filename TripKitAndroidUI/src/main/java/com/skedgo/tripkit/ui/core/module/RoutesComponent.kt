package com.skedgo.tripkit.ui.core.module
import com.skedgo.tripkit.ui.tripresults.TripResultListFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = arrayOf(
    RoutesModule::class,
    LocationStuffModule::class,
    CameraPositionDataModule::class
))
interface RoutesComponent {
  fun inject(fragment: TripResultListFragment)
}