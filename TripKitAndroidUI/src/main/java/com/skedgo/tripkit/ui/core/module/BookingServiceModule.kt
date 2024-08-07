package com.skedgo.tripkit.ui.core.module

import com.google.gson.GsonBuilder
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingApi
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingService
import com.skedgo.tripkit.ui.booking.apiv2.GsonAdaptersBookingV2LogTripResponse
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
class BookingServiceModule {
    @Provides
    fun bookingV2TrackingApi(
        builder: Retrofit.Builder,
        client: OkHttpClient?
    ): BookingV2TrackingApi? {
        val gson = GsonBuilder().registerTypeAdapterFactory(GsonAdaptersBookingV2LogTripResponse())
            .create()
        return builder.addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(BookingV2TrackingApi::class.java)
    }

    @Provides
    fun provideBookingV2TrackingService(api: BookingV2TrackingApi?): BookingV2TrackingService {
        return BookingV2TrackingService(api!!)
    }
}