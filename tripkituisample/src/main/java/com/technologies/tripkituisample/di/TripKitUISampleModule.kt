package com.technologies.tripkituisample.di

import android.content.Context
import android.content.res.Resources
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skedgo.tripkit.common.model.GsonAdaptersBooking
import com.skedgo.tripkit.common.util.LowercaseEnumTypeAdapterFactory
import com.skedgo.tripkit.configuration.Server
import com.skedgo.tripkit.data.clients.ClientsApi
import com.skedgo.tripkit.ui.core.settings.DeveloperPreferenceRepository
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsRepository
import com.technologies.tripkituisample.AppEventBus
import com.technologies.tripkituisample.TripKitUISampleActionButtonHandler
import com.technologies.tripkituisample.TripKitUISampleActionButtonHandlerFactory
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
class TripKitUISampleModule {
    @Provides
    fun resources(context: Context): Resources {
        return context.resources
    }

    @Singleton
    @Provides
    fun getGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapterFactory(LowercaseEnumTypeAdapterFactory())
            .registerTypeAdapterFactory(GsonAdaptersBooking())
            .create()
    }

    @Provides
    fun tripKitUISampleActionButtonHandler(
        eventBus: AppEventBus,
    ): TripKitUISampleActionButtonHandler = TripKitUISampleActionButtonHandler(eventBus)

    @Provides
    fun tripKitUISampleActionButtonHandlerFactory(provider: Provider<TripKitUISampleActionButtonHandler>): TripKitUISampleActionButtonHandlerFactory
            = TripKitUISampleActionButtonHandlerFactory(provider)

    @Singleton
    @Provides
    fun getClientsApi(
        httpClient: OkHttpClient,
        gson: Gson,
        developerPreferenceRepository: DeveloperPreferenceRepository
    ): ClientsApi {
        var server = developerPreferenceRepository.server
        if (server.isEmpty()) {
            server = Server.ApiTripGo.value
        }
        return Retrofit.Builder()
            .baseUrl(server)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .client(httpClient)
            .build()
            .create(ClientsApi::class.java)
    }
}