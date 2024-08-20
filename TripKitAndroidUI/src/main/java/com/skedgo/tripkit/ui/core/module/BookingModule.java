package com.skedgo.tripkit.ui.core.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skedgo.tripkit.booking.AuthApi;
import com.skedgo.tripkit.booking.BookingApi;
import com.skedgo.tripkit.booking.BookingService;
import com.skedgo.tripkit.booking.BookingServiceImpl;
import com.skedgo.tripkit.booking.ExternalOAuthService;
import com.skedgo.tripkit.booking.ExternalOAuthServiceGenerator;
import com.skedgo.tripkit.booking.ExternalOAuthServiceImpl;
import com.skedgo.tripkit.booking.FormField;
import com.skedgo.tripkit.booking.FormFieldJsonAdapter;
import com.skedgo.tripkit.booking.GsonAdaptersAuthProvider;
import com.skedgo.tripkit.booking.GsonAdaptersLogOutResponse;
import com.skedgo.tripkit.booking.GsonAdaptersQuickBooking;
import com.skedgo.tripkit.booking.QuickBookingApi;
import com.skedgo.tripkit.booking.QuickBookingService;
import com.skedgo.tripkit.booking.QuickBookingServiceImpl;
import com.skedgo.tripkit.booking.quickbooking.QuickBookingRepository;
import com.skedgo.tripkit.configuration.ServerManager;
import com.skedgo.tripkit.data.database.TripKitDatabase;
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingApi;
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingService;
import com.skedgo.tripkit.ui.booking.apiv2.GsonAdaptersBookingV2LogTripResponse;

import dagger.Module;
import dagger.Provides;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class BookingModule {
    @Provides
    BookingApi bookingApi(OkHttpClient httpClient) {
        final Gson gson = new GsonBuilder()
            .registerTypeAdapter(FormField.class, new FormFieldJsonAdapter())
            .create();
        return new Retrofit.Builder()
            /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.INSTANCE.getConfiguration().getApiTripGoUrl())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create(BookingApi.class);
    }

    @Provides
    QuickBookingApi quickBookingApi(OkHttpClient httpClient) {
        final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new GsonAdaptersQuickBooking())
            .create();
        return new Retrofit.Builder()
            /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.INSTANCE.getConfiguration().getApiTripGoUrl())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create(QuickBookingApi.class);
    }

    @Provides
    com.skedgo.tripkit.booking.quickbooking.QuickBookingApi newQuickBookingApi(
        OkHttpClient httpClient
    ) {
        return new Retrofit.Builder()
            /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.INSTANCE.getConfiguration().getApiTripGoUrl())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(com.skedgo.tripkit.booking.quickbooking.QuickBookingApi.class);
    }

    @Provides
    AuthApi authApi(OkHttpClient httpClient) {
        final Gson gson = new GsonBuilder()
            .registerTypeAdapter(FormField.class, new FormFieldJsonAdapter())
            .registerTypeAdapterFactory(new GsonAdaptersAuthProvider())
            .registerTypeAdapterFactory(new GsonAdaptersLogOutResponse())
            .create();
        return new Retrofit.Builder()
            /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.INSTANCE.getConfiguration().getApiTripGoUrl())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create(AuthApi.class);
    }
    /*
        @Provides
    fun logTripApi(builder: Retrofit.Builder, httpClient: OkHttpClient): BookingV2TrackingApi {
        val gson = GsonBuilder()
                .registerTypeAdapterFactory(GsonAdaptersBookingV2LogTripResponse())
                .create()
        return builder
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build()
                .create(BookingV2TrackingApi::class.java)
    }


     */

    @Provides
    BookingV2TrackingApi bookingV2TrackingApi(Retrofit.Builder builder, OkHttpClient client) {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonAdaptersBookingV2LogTripResponse()).create();
        return builder.addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(BookingV2TrackingApi.class);
    }

    @Provides
    BookingV2TrackingService provideBookingV2TrackingService(BookingV2TrackingApi api) {
        return new BookingV2TrackingService(api);
    }
//
//    @Provides AuthService authService(AuthApi authApi) {
//        return new AuthServiceImpl(authApi);
//    }
//
//    @Provides BookingViewModel bookingViewModel(BookingService bookingService) {
//        return new BookingViewModelImpl(bookingService);
//    }

    //    @Provides
//    AuthenticationViewModel authenticationViewModel() {
//        return new AuthenticationViewModelImpl();
//    }
//
    @Provides
    ExternalOAuthServiceGenerator provideExternalOAuthServiceGenerator() {
        return new ExternalOAuthServiceGenerator(new OkHttpClient.Builder());
    }

    @Provides
    ExternalOAuthService getExternalOAuthService(ExternalOAuthServiceGenerator externalOAuthServiceGenerator) {
        return new ExternalOAuthServiceImpl(externalOAuthServiceGenerator);
    }

    @Provides
    BookingService getBookingService(BookingApi bookingApi) {
        return new BookingServiceImpl(bookingApi, new Gson());
    }

    @Provides
    QuickBookingService getQuickBookingService(QuickBookingApi quickBookingApi) {
        return new QuickBookingServiceImpl(quickBookingApi);
    }

    @Provides
    com.skedgo.tripkit.booking.quickbooking.QuickBookingService getNewQuickBookingService(
        com.skedgo.tripkit.booking.quickbooking.QuickBookingApi quickBookingApi
    ) {
        return new com.skedgo.tripkit.booking.quickbooking.QuickBookingService.QuickBookingServiceImpl(
            quickBookingApi
        );
    }

    @Provides
    QuickBookingRepository getQuickBookingRepository(
        com.skedgo.tripkit.booking.quickbooking.QuickBookingService service,
        TripKitDatabase database
    ) {
        return new QuickBookingRepository(service, database);
    }
}
