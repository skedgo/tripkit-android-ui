package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.agenda.ConfigRepository
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepository
import com.skedgo.tripkit.data.database.locations.carparks.CarParkMapper
import com.skedgo.tripkit.data.database.locations.carparks.CarParkPersistor
import com.skedgo.tripkit.data.database.locations.carpods.CarPodMapper
import com.skedgo.tripkit.data.database.locations.carpods.CarPodRepository
import com.skedgo.tripkit.data.database.locations.facility.FacilityRepository
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingRepository
import com.skedgo.tripkit.data.database.locations.onstreetparking.OnStreetParkingMapper
import com.skedgo.tripkit.data.database.locations.onstreetparking.OnStreetParkingPersistor
import com.skedgo.tripkit.data.locations.LocationsApi
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.data.locations.StopsFetcher.ICellsLoader
import com.skedgo.tripkit.data.locations.StopsFetcher.ICellsPersistor
import com.skedgo.tripkit.data.locations.StopsFetcher.IStopsPersistor
import dagger.Module
import dagger.Provides

@Module
class ScheduledStopServiceModule {
    @Provides
    fun provideStopsFetcher(
        api: LocationsApi,
        cellsLoader: ICellsLoader,
        cellsPersistor: ICellsPersistor,
        stopsPersistor: IStopsPersistor,
        configCreator: ConfigRepository,
        CarParkMapper: CarParkMapper,
        carParkPersistor: CarParkPersistor,
        onStreetParkingPersistor: OnStreetParkingPersistor,
        onStreetParkingMapper: OnStreetParkingMapper,
        bikePodRepository: BikePodRepository,
        freeFloatingRepository: FreeFloatingRepository,
        carPodMapper: CarPodMapper,
        carPodRepository: CarPodRepository,
        facilityRepository: FacilityRepository
    ): StopsFetcher {
        return StopsFetcher(
            api,
            cellsLoader,
            cellsPersistor,
            stopsPersistor,
            configCreator,
            bikePodRepository,
            freeFloatingRepository,
            carParkPersistor,
            onStreetParkingPersistor,
            CarParkMapper,
            carPodMapper,
            onStreetParkingMapper,
            carPodRepository,
            facilityRepository
        )
    }
}
