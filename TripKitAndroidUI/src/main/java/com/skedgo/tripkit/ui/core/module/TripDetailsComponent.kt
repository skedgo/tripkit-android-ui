package com.skedgo.tripkit.ui.core.module
import com.skedgo.tripkit.ui.tripresult.TripResultMapContributor
import com.skedgo.tripkit.ui.tripresult.TripResultPagerFragment
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = arrayOf(
    TripDetailsModule::class,
        TripProgressModule::class,
        CameraPositionDataModule::class,
        TransportationIconTintStrategyModule::class))
interface TripDetailsComponent {
  fun inject(fragment: TripResultPagerFragment)
  fun inject(fragment: TripSegmentListFragment)
  fun inject(contributor: TripResultMapContributor)
}
