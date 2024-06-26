package com.skedgo.tripkit.ui.core.module;

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.google.gson.Gson
import com.skedgo.tripkit.AndroidGeocoder
import com.skedgo.tripkit.ServiceApi
import com.skedgo.tripkit.bookingproviders.BookingResolver
import com.skedgo.tripkit.bookingproviders.BookingResolverImpl
import com.skedgo.tripkit.common.util.Gsons
import com.skedgo.tripkit.configuration.ServerManager
import com.skedgo.tripkit.data.database.TripKitDatabase
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepository
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepositoryImpl
import com.skedgo.tripkit.data.database.locations.facility.FacilityRepository
import com.skedgo.tripkit.data.database.locations.facility.FacilityRepositoryImpl
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingRepository
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingRepositoryImpl
import com.skedgo.tripkit.data.locations.LocationsApi
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.utils.actionhandler.TKUIActionButtonHandler
import com.skedgo.tripkit.ui.controller.utils.actionhandler.TKUIActionButtonHandlerFactory
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
import javax.inject.Provider
import javax.inject.Singleton


@Module
class TripKitUIModule {

    @Provides
    internal fun bindApplication(app: Application): Application = app

    @Provides
    @Singleton
    internal fun bus(errorLogger: ErrorLogger): Bus =
    MainThreadBus { errorLogger.logError(it) }

//    @Provides
//    internal fun httpClient3(): OkHttpClient = TripKit.getInstance().okHttpClient3

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
    @Singleton
    internal fun provideFacilityRepositoryImpl(tripGoDatabase2: TripKitDatabase): FacilityRepository {
        return FacilityRepositoryImpl(tripGoDatabase2)
    }

    @Provides
    internal fun gson(): Gson = Gsons.createForLowercaseEnum()
    @Provides
    @Singleton
    internal fun locationsApi(httpClient: OkHttpClient, gson: Gson): LocationsApi {
        return Retrofit.Builder()
                /* This base url is ignored as the api relies on @Url. */
                .baseUrl(ServerManager.configuration.apiTripGoUrl)
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
                .baseUrl(ServerManager.configuration.apiTripGoUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(httpClient)
                .build()
                .create(ServiceApi::class.java)
    }

    @Provides
    fun getBookingResolver(context: Context): BookingResolver {
        return BookingResolverImpl(
            context.resources,
            context.packageManager,
            AndroidGeocoder(context)
        )
    }

    @Provides
    @Singleton
    fun provideViewControllerEventBus() = ViewControllerEventBus

    @Provides
    fun tkuiActionButtonHandler(): TKUIActionButtonHandler = TKUIActionButtonHandler(ViewControllerEventBus)

    @Provides
    fun tkuiActionButtonHandlerFactory(provider: Provider<TKUIActionButtonHandler>): TKUIActionButtonHandlerFactory = TKUIActionButtonHandlerFactory(provider)

}