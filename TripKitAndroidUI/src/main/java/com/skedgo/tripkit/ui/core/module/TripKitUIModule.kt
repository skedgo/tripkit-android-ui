package com.skedgo.tripkit.ui.core.module;

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skedgo.TripKit
import com.skedgo.routepersistence.LocationTypeAdapterFactory
import com.skedgo.routepersistence.RouteDatabaseHelper
import com.skedgo.routepersistence.RouteStore
import com.skedgo.routepersistence.RoutingStatusStore
import com.skedgo.tripkit.ServiceApi
import com.skedgo.tripkit.common.model.GsonAdaptersBooking
import com.skedgo.tripkit.common.model.GsonAdaptersRealtimeAlert
import com.skedgo.tripkit.common.util.Gsons
import com.skedgo.tripkit.common.util.LowercaseEnumTypeAdapterFactory
import com.skedgo.tripkit.configuration.Server
import com.skedgo.tripkit.data.database.TripKitDatabase
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepository
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepositoryImpl
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingRepository
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingRepositoryImpl
import com.skedgo.tripkit.data.locations.LocationsApi
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.data.routingstatus.RoutingStatusRepositoryImpl
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.ui.core.CellsLoader
import com.skedgo.tripkit.ui.core.CellsPersistor
import com.skedgo.tripkit.ui.core.StopsPersistor
import com.skedgo.tripkit.ui.map.ScheduledStopRepository
import com.skedgo.tripkit.ui.utils.MainThreadBus
import com.squareup.otto.Bus
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
class TripKitUIModule {

    @Provides
    internal fun bindApplication(app: Application): Application = app

    @Provides
    @Singleton
    internal fun bus(errorLogger: ErrorLogger): Bus =
    MainThreadBus { errorLogger.logError(it) }

    @Provides
    internal fun httpClient3(): OkHttpClient = TripKit.getInstance().okHttpClient3

    @Provides
    internal fun resources(appContext: Context): Resources = appContext.resources

    @Provides
    internal fun provideStopsPersistor(
            context: Context,
            gson: Gson,
            scheduledStopRepository: ScheduledStopRepository): StopsFetcher.IStopsPersistor {
        return StopsPersistor(context, gson, scheduledStopRepository)
    }

    @Provides
    @Singleton
    internal fun bikePodRepositoryImpl(tripGoDatabase2: TripKitDatabase): BikePodRepository {
        return BikePodRepositoryImpl(tripGoDatabase2)
    }

    @Provides
    @Singleton
    internal fun freeFloatingRepositoryImpl(tripGoDatabase2: TripKitDatabase): FreeFloatingRepository {
        return FreeFloatingRepositoryImpl(tripGoDatabase2)
    }

    @Provides
    internal fun provideCellsPersistor(context: Context): StopsFetcher.ICellsPersistor {
        return CellsPersistor(context)
    }

    @Provides
    internal fun provideCellsLoader(context: Context): StopsFetcher.ICellsLoader {
        return CellsLoader(context)
    }

    @Provides
    internal fun gson(): Gson = Gsons.createForLowercaseEnum()
    @Provides
    @Singleton
    internal fun locationsApi(httpClient: OkHttpClient, gson: Gson): LocationsApi {
        return Retrofit.Builder()
                /* This base url is ignored as the api relies on @Url. */
                .baseUrl(Server.ApiTripGo.value)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build()
                .create(LocationsApi::class.java)
    }

    @Singleton
    @Provides
    fun getServiceApi(httpClient: OkHttpClient, gson: Gson): ServiceApi {
        return Retrofit.Builder()
                .baseUrl(Server.ApiTripGo.value)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(httpClient)
                .build()
                .create(ServiceApi::class.java)
    }


    @Provides
    @Singleton
    internal fun routingStatusStore(
            routeDatabaseHelper: RouteDatabaseHelper
    ): RoutingStatusStore = RoutingStatusStore(routeDatabaseHelper)

    @Provides
    internal fun routingStatusRepository(
            impl: RoutingStatusRepositoryImpl
    ): RoutingStatusRepository = impl

}