package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.data.database.locations.carparks.ParkingModule
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import dagger.Subcomponent


@ActivityScope
@Subcomponent(modules = arrayOf(HomeMapFragmentModule::class,
        CameraPositionDataModule::class,
        LocationStuffModule::class,
        PinUpdateRepositoryModule::class,
        ParkingModule::class,
        ScheduledStopServiceModule::class))
interface HomeMapFragmentComponent {
    fun inject(fragment: TripKitMapFragment)
}