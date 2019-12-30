package com.skedgo.tripkit.ui.core.module;

import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepository;
import com.skedgo.tripkit.data.database.locations.carparks.CarParkMapper;
import com.skedgo.tripkit.data.database.locations.carparks.CarParkPersistor;
import com.skedgo.tripkit.data.database.locations.carpods.CarPodMapper;
import com.skedgo.tripkit.data.database.locations.carpods.CarPodRepository;
import com.skedgo.tripkit.data.database.locations.onstreetparking.OnStreetParkingMapper;
import com.skedgo.tripkit.data.database.locations.onstreetparking.OnStreetParkingPersistor;
import com.skedgo.tripkit.data.locations.LocationsApi;
import com.skedgo.tripkit.data.locations.StopsFetcher;
import dagger.Module;
import dagger.Provides;
import com.skedgo.tripkit.agenda.ConfigRepository;

@Module
public class ScheduledStopServiceModule {
  @Provides
  StopsFetcher provideStopsFetcher(LocationsApi api,
                                   StopsFetcher.ICellsLoader cellsLoader,
                                   StopsFetcher.ICellsPersistor cellsPersistor,
                                   StopsFetcher.IStopsPersistor stopsPersistor,
                                   ConfigRepository configCreator,
                                   CarParkMapper CarParkMapper,
                                   CarParkPersistor carParkPersistor,
                                   OnStreetParkingPersistor onStreetParkingPersistor,
                                   OnStreetParkingMapper onStreetParkingMapper,
                                   BikePodRepository bikePodRepository,
                                   CarPodMapper carPodMapper,
                                   CarPodRepository carPodRepository) {
    return new StopsFetcher(
        api,
        cellsLoader,
        cellsPersistor,
        stopsPersistor,
        configCreator,
        bikePodRepository,
        carParkPersistor,
        onStreetParkingPersistor,
        CarParkMapper,
        carPodMapper,
        onStreetParkingMapper,
        carPodRepository
    );
  }
}
