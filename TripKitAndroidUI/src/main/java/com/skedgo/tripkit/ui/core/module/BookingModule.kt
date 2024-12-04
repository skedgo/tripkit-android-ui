package com.skedgo.tripkit.ui.core.module

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skedgo.tripkit.account.data.GsonAdaptersLogOutResponse
import com.skedgo.tripkit.booking.AuthApi
import com.skedgo.tripkit.booking.BookingApi
import com.skedgo.tripkit.booking.BookingService
import com.skedgo.tripkit.booking.BookingServiceImpl
import com.skedgo.tripkit.booking.ExternalOAuthService
import com.skedgo.tripkit.booking.ExternalOAuthServiceGenerator
import com.skedgo.tripkit.booking.ExternalOAuthServiceImpl
import com.skedgo.tripkit.booking.FormField
import com.skedgo.tripkit.booking.FormFieldJsonAdapter
import com.skedgo.tripkit.booking.GsonAdaptersAuthProvider
import com.skedgo.tripkit.booking.GsonAdaptersQuickBooking
import com.skedgo.tripkit.booking.QuickBookingApi
import com.skedgo.tripkit.booking.QuickBookingService
import com.skedgo.tripkit.booking.QuickBookingServiceImpl
import com.skedgo.tripkit.booking.quickbooking.QuickBookingRepository
import com.skedgo.tripkit.configuration.ServerManager
import com.skedgo.tripkit.data.database.TripKitDatabase
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingApi
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingService
import com.skedgo.tripkit.ui.booking.apiv2.GsonAdaptersBookingV2LogTripResponse
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Module
class BookingModule {
    @Provides
    fun bookingApi(httpClient: OkHttpClient): BookingApi {
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(FormField::class.java, FormFieldJsonAdapter())
            .create()
        return Retrofit.Builder() /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.configuration.apiTripGoUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create<BookingApi>(BookingApi::class.java)
    }

    @Provides
    fun quickBookingApi(httpClient: OkHttpClient): QuickBookingApi {
        val gson: Gson = GsonBuilder()
            .registerTypeAdapterFactory(GsonAdaptersQuickBooking())
            .create()
        return Retrofit.Builder() /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.configuration.apiTripGoUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create<QuickBookingApi>(QuickBookingApi::class.java)
    }

    @Provides
    fun newQuickBookingApi(
        httpClient: OkHttpClient
    ): com.skedgo.tripkit.booking.quickbooking.QuickBookingApi {
        return Retrofit.Builder() /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.configuration.apiTripGoUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create<com.skedgo.tripkit.booking.quickbooking.QuickBookingApi>(com.skedgo.tripkit.booking.quickbooking.QuickBookingApi::class.java)
    }

    @Provides
    fun authApi(httpClient: OkHttpClient): AuthApi {
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(FormField::class.java, FormFieldJsonAdapter())
            .registerTypeAdapterFactory(GsonAdaptersAuthProvider())
            .registerTypeAdapterFactory(GsonAdaptersLogOutResponse())
            .create()
        return Retrofit.Builder() /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.configuration.apiTripGoUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create<AuthApi>(AuthApi::class.java)
    }

    @Provides
    fun bookingV2TrackingApi(
        builder: Retrofit.Builder,
        client: OkHttpClient
    ): BookingV2TrackingApi {
        val gson: Gson =
            GsonBuilder().registerTypeAdapterFactory(GsonAdaptersBookingV2LogTripResponse())
                .create()
        return builder.addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create<BookingV2TrackingApi>(BookingV2TrackingApi::class.java)
    }

    @Provides
    fun provideBookingV2TrackingService(api: BookingV2TrackingApi): BookingV2TrackingService {
        return BookingV2TrackingService(api)
    }

    @Provides
    fun provideExternalOAuthServiceGenerator(): ExternalOAuthServiceGenerator {
        return ExternalOAuthServiceGenerator(Builder())
    }

    @Provides
    fun getExternalOAuthService(externalOAuthServiceGenerator: ExternalOAuthServiceGenerator): ExternalOAuthService {
        return ExternalOAuthServiceImpl(externalOAuthServiceGenerator)
    }

    @Provides
    fun getBookingService(bookingApi: BookingApi): BookingService {
        return BookingServiceImpl(bookingApi, Gson())
    }

    @Provides
    fun getQuickBookingService(quickBookingApi: QuickBookingApi): QuickBookingService {
        return QuickBookingServiceImpl(quickBookingApi)
    }

    @Provides
    fun getNewQuickBookingService(
        quickBookingApi: com.skedgo.tripkit.booking.quickbooking.QuickBookingApi
    ): com.skedgo.tripkit.booking.quickbooking.QuickBookingService {
        return com.skedgo.tripkit.booking.quickbooking.QuickBookingService.QuickBookingServiceImpl(
            quickBookingApi
        )
    }

    @Provides
    fun getQuickBookingRepository(
        service: com.skedgo.tripkit.booking.quickbooking.QuickBookingService,
        database: TripKitDatabase
    ): QuickBookingRepository {
        return QuickBookingRepository(service, database)
    }
}
