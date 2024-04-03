package com.skedgo.tripkit.ui.core.module

import com.google.gson.Gson
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.data.realtime.LatestApi
import com.skedgo.tripkit.ui.data.realtime.RealTimeRepositoryImpl
import com.skedgo.tripkit.ui.data.realtime.RealtimeAlertRepositoryImpl
import com.skedgo.tripkit.ui.realtime.RealTimeRepository
import com.skedgo.tripkit.ui.realtime.RealtimeAlertRepository
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import com.skedgo.tripkit.configuration.ServerManager
import javax.inject.Singleton

@Module
class RealTimeRepositoryModule {

  @Provides
  @Singleton
  fun realTimeAlertRepository(impl: RealtimeAlertRepositoryImpl): RealtimeAlertRepository = impl

  @Provides
  @Singleton
  fun realTimeRepository(impl: RealTimeRepositoryImpl): RealTimeRepository = impl

  @Provides
  internal fun latestApi(
          gson: Gson,
          httpClient: OkHttpClient,
          schedulers: SchedulerFactory
  ): LatestApi = Retrofit.Builder()
      /* This base url is ignored as the api relies on @Url. */
      .baseUrl(ServerManager.configuration.apiTripGoUrl)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(schedulers.ioScheduler))
      .addConverterFactory(GsonConverterFactory.create(gson))
      .client(httpClient)
      .build()
      .create(LatestApi::class.java)
}