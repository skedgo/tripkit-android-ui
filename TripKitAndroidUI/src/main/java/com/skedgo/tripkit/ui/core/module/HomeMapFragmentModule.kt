package com.skedgo.tripkit.ui.core.module

import android.view.LayoutInflater
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import dagger.Module
import dagger.Provides

@Module(
    includes = [ServiceAlertDataModule::class,
        CameraPositionDataModule::class,
        DefaultStopInfoWindowAdapterModule::class,
        DefaultLoadPOILocationsByViewPortModule::class]
)
class HomeMapFragmentModule(fragment: TripKitMapFragment) {
    private val inflater = fragment.layoutInflater

    @Provides
    fun layoutInflater(): LayoutInflater = inflater
}
