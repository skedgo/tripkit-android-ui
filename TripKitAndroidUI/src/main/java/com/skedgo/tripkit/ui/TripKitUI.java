package com.skedgo.tripkit.ui;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.annotation.Nullable;

import com.google.android.libraries.places.api.Places;
import com.skedgo.DaggerTripKit;
import com.skedgo.TripKit;
import com.skedgo.routepersistence.RouteStore;
import com.skedgo.tripkit.*;
import com.skedgo.tripkit.data.database.DbHelper;
import com.skedgo.tripkit.data.regions.RegionService;
import com.skedgo.tripkit.notification.NotificationKt;
import com.skedgo.tripkit.regionrouting.RegionRoutingAutoCompleter;
import com.skedgo.tripkit.regionrouting.RegionRoutingRepository;
import com.skedgo.tripkit.routing.GeoLocation;
import com.skedgo.tripkit.routing.GetOffAlertCache;
import com.skedgo.tripkit.ui.controller.ControllerComponent;
import com.skedgo.tripkit.ui.controller.ControllerModule;
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment;
import com.skedgo.tripkit.ui.core.module.*;
import com.skedgo.tripkit.ui.core.settings.DeveloperPreferenceRepositoryImpl;
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository;
import com.skedgo.tripkit.ui.data.waypoints.WaypointsModule;
import com.skedgo.tripkit.ui.locationpointer.LocationPointerComponent;
import com.skedgo.tripkit.ui.map.MarkerIconManager;
import com.skedgo.tripkit.ui.poidetails.PoiDetailsFragment;
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository;
import com.skedgo.tripkit.ui.search.FetchSuggestions;
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailFragment;
import com.skedgo.tripkit.ui.timetables.TimetableFragment;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;
import com.uber.rxdogtag.RxDogTag;

import dagger.Component;

import net.danlew.android.joda.JodaTimeAndroid;

import okhttp3.OkHttpClient;

import com.skedgo.tripkit.ui.core.module.CyclingSpeedRepositoryModule;

import skedgo.tripgo.agenda.legacy.GetRoutingConfigModule;
import skedgo.tripgo.agenda.legacy.WalkingSpeedRepositoryModule;

import com.skedgo.tripkit.configuration.Key;
import com.skedgo.tripkit.logging.ErrorLogger;

import timber.log.Timber;

import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.skedgo.tripkit.routing.TripAlarmBroadcastReceiver.NOTIFICATION_CHANNEL_START_TRIP;
import static com.skedgo.tripkit.routing.TripAlarmBroadcastReceiver.NOTIFICATION_CHANNEL_START_TRIP_ID;

@Singleton
@Component(modules = {
        AutoCompleteTaskModule.class,
        SchedulerFactoryModule.class,
        GooglePlacesModule.class,
        ConnectivityServiceModule.class,
        FetchSuggestionsModule.class,
        RouteStoreModule.class,
        TripKitUIModule.class,
        ContextModule.class,
        HttpClientModule.class,
        ErrorLoggerModule.class,
        PicassoModule.class,
        TripKitModule.class,
        DbHelperModule.class,
        ServiceViewModelModule.class,
        RealTimeRepositoryModule.class,
        ServiceAlertDataModule.class,
        ServiceDetailsModule.class,
        ServiceDetailItemViewModelModule.class,
        TripGroupRepositoryModule.class,
        DeparturesModule.class,
        EventTrackerModule.class,
        LocationStuffModule.class,
        MyPersonalDataModule.class,
        PreferredTransferTimeRepositoryModule.class,
        CyclingSpeedRepositoryModule.class,
        WalkingSpeedRepositoryModule.class,
        PrioritiesRepositoryModule.class,
        GetRoutingConfigModule.class,
        BookingModule.class,
        SchedulerFactoryModule.class,
        WaypointsModule.class,
        FavoriteTripsModule.class,
        UserInfoRepositoryModule.class,
        ViewModelModule.class,
        ControllerModule.class,
        DeveloperOptionModule.class,
        RemindersRepositoryModule.class
})
public abstract class TripKitUI {
    private static TripKitUI instance;
    public static String AUTHORITY_END = ".com.skedgo.tripkit.ui.";

    public static TripKitUI getInstance() {
        synchronized (TripKitUI.class) {
            if (instance == null) {
                throw new IllegalStateException("Must initialize TripKitUI before using getInstance()");
            }

            return instance;
        }
    }

    public static Configs buildTripKitConfig(Context context, Key.ApiKey key) {
        DeveloperPreferenceRepositoryImpl repository = new DeveloperPreferenceRepositoryImpl(context, context.getSharedPreferences(
                "TripKit", Context.MODE_PRIVATE));
        boolean isDebuggable = (0 != (context.getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) || BuildConfig.DEBUG);
        return TripKitConfigs.builder().context(context)

                .debuggable(isDebuggable)
                .baseUrlAdapterFactory(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return repository.getServer();
                    }
                })
                .userTokenProvider(() -> {
                    SharedPreferences prefs = context.getSharedPreferences("UserTokenPreferences", Context.MODE_PRIVATE);
                    return prefs.getString("userToken", "");
                })
                .key(() -> key).build();
    }

    public static Configs buildTripKitConfig(
            Context context,
            Key.ApiKey key,
            @Nullable Callable<String> customUrlAdapterFactory
    ) {
        DeveloperPreferenceRepositoryImpl repository = new DeveloperPreferenceRepositoryImpl(context, context.getSharedPreferences(
                "TripKit", Context.MODE_PRIVATE));
        boolean isDebuggable = (0 != (context.getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) || BuildConfig.DEBUG);
        return TripKitConfigs.builder().context(context)
                .debuggable(isDebuggable)
                .baseUrlAdapterFactory(
                        (customUrlAdapterFactory != null) ?
                                customUrlAdapterFactory :
                                (Callable<String>) repository::getServer

                )
                .userTokenProvider(() -> {
                    SharedPreferences prefs = context.getSharedPreferences("UserTokenPreferences", Context.MODE_PRIVATE);
                    return prefs.getString("userToken", "");
                })
                .key(() -> key).build();
    }

    public static void initialize(Context context, Key.ApiKey key, @Nullable Configs configs) {
        initialize(context, key, configs, null);
    }

    public static void initialize(Context context, Key.ApiKey key,
                                  @Nullable Configs configs,
                                  @Nullable HttpClientModule httpClientModule,
                                  String placesApiKey) {
        initialize(context, key, configs, httpClientModule);
        if (!Places.isInitialized()) {
            Places.initialize(context, placesApiKey);
        }
    }

    public static void initialize(Context context, Key.ApiKey key,
                                  @Nullable Configs configs,
                                  @Nullable HttpClientModule httpClientModule) {
        RxDogTag.install();
        MarkerIconManager.INSTANCE.init(context);
        if (!TripKit.isInitialized()) {
            if ("SKEDGO_API_KEY".equals(key.getValue())) {
                throw new IllegalStateException("Invalid SkedGo API Key.");
            }

            Configs tripKitConfigs = configs;
            if (tripKitConfigs == null) {
                tripKitConfigs = buildTripKitConfig(context, key);
            }

            if (httpClientModule != null) {
                TripKit tripKit = DaggerTripKit.builder()
                        .mainModule(new MainModule(tripKitConfigs))
                        .httpClientModule(httpClientModule)
                        .build();
                TripKit.initialize(context, tripKit);
                JodaTimeAndroid.init(context);
                GetOffAlertCache.INSTANCE.init(context);
                GeoLocation.INSTANCE.init(context);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    List<NotificationChannel> channels = new ArrayList<>();
                    channels.add(NotificationKt.createChannel(
                            NOTIFICATION_CHANNEL_START_TRIP_ID,
                            NOTIFICATION_CHANNEL_START_TRIP)
                    );
                    NotificationKt.createNotificationChannels(
                            context,
                            channels
                    );
                }
            } else {
                TripKit.initialize(tripKitConfigs);
            }

            if (!Places.isInitialized()) {
                String placesApiKey = context.getString(R.string.google_places_api_key);
                if (!placesApiKey.equals("GOOGLE_PLACES_API_KEY")) {
                    Places.initialize(context, placesApiKey);
                }
            }

            if (tripKitConfigs.debuggable()) {
                Timber.plant(new Timber.DebugTree());
            }

            DaggerTripKitUI.Builder builder = DaggerTripKitUI.builder();
            if (httpClientModule != null) {
                builder.httpClientModule(httpClientModule);
            } else {
                builder.httpClientModule(new HttpClientModule(
                        null, null,
                        tripKitConfigs,
                        null, null
                ));
            }

            instance = builder.contextModule(new ContextModule(context))
                    .build();
        }
    }

    public abstract RouteInputViewComponent routeInputViewComponent();

    public abstract TripSegmentViewModelComponent tripSegmentViewModelComponent();

    public abstract TimePickerComponent timePickerComponent();

    public abstract TripDetailsComponent tripDetailsComponent();

    public abstract HomeMapFragmentComponent homeMapFragmentComponent(HomeMapFragmentModule module);

    public abstract ServiceStopMapComponent serviceStopMapComponent();

    public abstract RoutesComponent routesComponent();

    public abstract LocationSearchComponent locationSearchComponent();

    public abstract TripPreviewComponent tripPreviewComponent();

    public abstract AutoCompleteRoutingComponent autoCompleteRoutingComponent();

    public abstract LocationPointerComponent locationPointerComponent();

    public abstract ControllerComponent controllerComponent();

    public abstract Bus bus();

    public abstract OkHttpClient httpClient();

    public abstract Context appContext();

    public abstract RegionService regionService();

    public abstract FetchSuggestions fetchSuggestions();

    public abstract Picasso picasso();

    public abstract ErrorLogger errorLogger();

    public abstract PlaceSearchRepository searchRepository();

    public abstract DbHelper dbHelper();

    public abstract TripGroupRepository tripGroupRepository();

    public abstract RouteStore routeStore();

    public abstract RegionRoutingRepository regionRoutingRepository();

    public abstract RegionRoutingAutoCompleter regionRoutingAutoCompleter();

    public abstract void inject(TimetableFragment fragment);

    public abstract void inject(ServiceDetailFragment fragment);

    public abstract void inject(PoiDetailsFragment fragment);
}