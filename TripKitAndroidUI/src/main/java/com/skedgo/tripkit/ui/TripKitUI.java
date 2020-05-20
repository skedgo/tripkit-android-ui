package com.skedgo.tripkit.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import com.google.android.libraries.places.api.Places;
import com.skedgo.TripKit;
import com.skedgo.routepersistence.RouteStore;
import com.skedgo.tripkit.Configs;
import com.skedgo.tripkit.data.database.DbHelper;
import com.skedgo.tripkit.data.regions.RegionService;
import com.skedgo.tripkit.ui.core.module.*;
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository;
import com.skedgo.tripkit.ui.poidetails.PoiDetailsFragment;
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider;
import com.skedgo.tripkit.ui.provider.ServiceStopsProvider;
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository;
import com.skedgo.tripkit.ui.search.FetchSuggestions;
import com.skedgo.tripkit.ui.search.LocationSearchFragment;
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailFragment;
import com.skedgo.tripkit.ui.timetables.TimetableFragment;
import com.skedgo.tripkit.ui.timetables.TimetableMapContributor;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;
import com.uber.rxdogtag.RxDogTag;
import dagger.Component;
import okhttp3.OkHttpClient;
import skedgo.tripgo.agenda.legacy.CyclingSpeedRepositoryModule;
import skedgo.tripgo.agenda.legacy.GetRoutingConfigModule;
import skedgo.tripgo.agenda.legacy.WalkingSpeedRepositoryModule;
import com.skedgo.tripkit.configuration.Key;
import com.skedgo.tripkit.logging.ErrorLogger;
import timber.log.Timber;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        AutoCompleteTaskModule.class,
        SchedulerFactoryModule.class,
        GooglePlacesModule.class,
        ConnectivityServiceModule.class,
        FetchSuggestionsModule.class,
        TripKitUIModule.class,
        ContextModule.class,
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
        GetRoutingConfigModule.class
        })
public abstract class TripKitUI {
    private static TripKitUI instance;
    public static String AUTHORITY;

    public static TripKitUI getInstance() {
        synchronized (TripKitUI.class) {
            if (instance == null) {
                throw new IllegalStateException("Must initialize TripKitUI before using getInstance()");
            }

            return instance;
        }
    }

    public static void initialize(Context context, Key key) {
        RxDogTag.install();
        if (!TripKit.isInitialized()) {
            AUTHORITY =  context.getPackageName() + ".com.skedgo.tripkit.ui.";
            boolean isDebuggable = (0 != (context.getApplicationInfo().flags
                    & ApplicationInfo.FLAG_DEBUGGABLE ) || BuildConfig.DEBUG);

            Configs configs = Configs.builder()
                    .context(context)
                    .debuggable(isDebuggable)
                    .key(() -> key)
                    .build();
            TripKit.initialize(configs);
            if (!Places.isInitialized()) {
                String placesApiKey = context.getString(R.string.google_places_api_key);
                if (placesApiKey != "GOOGLE_PLACES_API_KEY") {
                    Places.initialize(context, placesApiKey);
                }
            }

            if (isDebuggable) {
            Timber.plant(new Timber.DebugTree());
        }
        instance = DaggerTripKitUI.builder()
                .contextModule(new ContextModule(context))
                .build();
        }
    }

    public abstract TimetableComponent timetableComponent(TimetableModule module);
    public abstract RouteInputViewComponent routeInputViewComponent();
    public abstract TripSegmentViewModelComponent tripSegmentViewModelComponent();
    public abstract TimePickerComponent timePickerComponent();

    public abstract TripDetailsComponent tripDetailsComponent();

    public abstract HomeMapFragmentComponent homeMapFragmentComponent(HomeMapFragmentModule module);
    public abstract ServiceStopMapComponent serviceStopMapComponent();
    public abstract RoutesComponent routesComponent();
    public abstract LocationSearchComponent locationSearchComponent();

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

    public abstract void inject(TimetableFragment fragment);
    public abstract void inject(ServiceStopsProvider provider);
    public abstract void inject(ServiceDetailFragment fragment);
    public abstract void inject(ScheduledStopsProvider provider);
    public abstract void inject(PoiDetailsFragment fragment);
}