package com.skedgo.tripkit.ui.core.module

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.skedgo.tripkit.location.UserGeoPointRepository
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.data.location.RxFusedLocationProviderClient
import com.skedgo.tripkit.ui.data.location.UserGeoPointRepositoryImpl
import dagger.Module
import dagger.Provides
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Provider

@Module
class LocationStuffModule {
  @Provides fun rxFusedLocationProviderClient(context: Context): RxFusedLocationProviderClient
          = RxFusedLocationProviderClient(FusedLocationProviderClient(context))

  @Provides
  fun locationStream(
          client: RxFusedLocationProviderClient
  ): Observable<Location> = client.requestLocationStream(
          LocationRequest.create()
                  .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                  .setInterval(TimeUnit.SECONDS.toMillis(30))
                  .setFastestInterval(TimeUnit.SECONDS.toMillis(10))
  )
  @Provides
  fun userGeoPointRepository(
          getLocationUpdates: Provider<Observable<Location>>,
          getNow: GetNow
  ): UserGeoPointRepository = UserGeoPointRepositoryImpl(
      getLocationUpdates = { getLocationUpdates.get() },
      getNow = getNow
  )
}
