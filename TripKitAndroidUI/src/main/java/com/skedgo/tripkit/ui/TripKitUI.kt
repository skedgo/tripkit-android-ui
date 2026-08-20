package com.skedgo.tripkit.ui

import android.app.NotificationChannel
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.google.android.libraries.places.api.Places
import com.skedgo.DaggerTripKit
import com.skedgo.TripKit.Companion.initialize
import com.skedgo.TripKit.Companion.isInitialized
import com.skedgo.routepersistence.RouteStore
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.HttpClientModule
import com.skedgo.tripkit.MainModule
import com.skedgo.tripkit.TripKitConfigs.Companion.builder
import com.skedgo.tripkit.configuration.Key.ApiKey
import com.skedgo.tripkit.data.TripKitKeys.getGooglePlacesApiKey
import com.skedgo.tripkit.data.database.DbHelper
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.notification.createChannel
import com.skedgo.tripkit.notification.createNotificationChannels
import com.skedgo.tripkit.regionrouting.RegionRoutingAutoCompleter
import com.skedgo.tripkit.regionrouting.RegionRoutingRepository
import com.skedgo.tripkit.routing.GeoLocation
import com.skedgo.tripkit.routing.GetOffAlertCache
import com.skedgo.tripkit.routing.TripAlarmBroadcastReceiver
import com.skedgo.tripkit.ui.controller.ControllerComponent
import com.skedgo.tripkit.ui.controller.ControllerModule
import com.skedgo.tripkit.ui.core.module.AutoCompleteRoutingComponent
import com.skedgo.tripkit.ui.core.module.AutoCompleteTaskModule
import com.skedgo.tripkit.ui.core.module.AutoCompleteTaskProvidesModule
import com.skedgo.tripkit.ui.core.module.BookingModule
import com.skedgo.tripkit.ui.core.module.ConnectivityServiceModule
import com.skedgo.tripkit.ui.core.module.ContextModule
import com.skedgo.tripkit.ui.core.module.CyclingSpeedRepositoryModule
import com.skedgo.tripkit.ui.core.module.DbHelperModule
import com.skedgo.tripkit.ui.core.module.DeparturesModule
import com.skedgo.tripkit.ui.core.module.DeveloperOptionModule
import com.skedgo.tripkit.ui.core.module.ErrorLoggerModule
import com.skedgo.tripkit.ui.core.module.EventTrackerModule
import com.skedgo.tripkit.ui.core.module.FavoriteTripsModule
import com.skedgo.tripkit.ui.core.module.FavoritesModule
import com.skedgo.tripkit.ui.core.module.FetchSuggestionsModule
import com.skedgo.tripkit.ui.core.module.GooglePlacesModule
import com.skedgo.tripkit.ui.core.module.HomeMapFragmentComponent
import com.skedgo.tripkit.ui.core.module.HomeMapFragmentModule
import com.skedgo.tripkit.ui.core.module.LocationSearchComponent
import com.skedgo.tripkit.ui.core.module.LocationStuffModule
import com.skedgo.tripkit.ui.core.module.MyPersonalDataModule
import com.skedgo.tripkit.ui.core.module.PicassoModule
import com.skedgo.tripkit.ui.core.module.PreferredTransferTimeRepositoryModule
import com.skedgo.tripkit.ui.core.module.PrioritiesRepositoryModule
import com.skedgo.tripkit.ui.core.module.RealTimeRepositoryModule
import com.skedgo.tripkit.ui.core.module.RemindersRepositoryModule
import com.skedgo.tripkit.ui.core.module.RouteInputViewComponent
import com.skedgo.tripkit.ui.core.module.RouteStoreModule
import com.skedgo.tripkit.ui.core.module.RoutesComponent
import com.skedgo.tripkit.ui.core.module.SchedulerFactoryModule
import com.skedgo.tripkit.ui.core.module.ServiceAlertDataModule
import com.skedgo.tripkit.ui.core.module.ServiceDetailItemViewModelModule
import com.skedgo.tripkit.ui.core.module.ServiceDetailsModule
import com.skedgo.tripkit.ui.core.module.ServiceStopMapComponent
import com.skedgo.tripkit.ui.core.module.ServiceViewModelModule
import com.skedgo.tripkit.ui.core.module.TimePickerComponent
import com.skedgo.tripkit.ui.core.module.TripDetailsComponent
import com.skedgo.tripkit.ui.core.module.TripGroupRepositoryModule
import com.skedgo.tripkit.ui.core.module.TripKitModule
import com.skedgo.tripkit.ui.core.module.TripKitPreferenceModule
import com.skedgo.tripkit.ui.core.module.TripKitUIModule
import com.skedgo.tripkit.ui.core.module.TripPreviewComponent
import com.skedgo.tripkit.ui.core.module.TripSegmentViewModelComponent
import com.skedgo.tripkit.ui.core.module.UserInfoRepositoryModule
import com.skedgo.tripkit.ui.core.module.ViewModelModule
import com.skedgo.tripkit.ui.core.settings.DeveloperPreferenceRepositoryImpl
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.data.waypoints.WaypointsModule
import com.skedgo.tripkit.ui.locationpointer.LocationPointerComponent
import com.skedgo.tripkit.ui.map.MarkerIconManager
import com.skedgo.tripkit.ui.poidetails.PoiDetailsFragment
import com.skedgo.tripkit.ui.poidetails.PoiDetailsMapContributor
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.routing.settings.UnitsRepository
import com.skedgo.tripkit.ui.search.FetchSuggestions
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailFragment
import com.skedgo.tripkit.ui.timetables.TimetableFragment
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import com.uber.rxdogtag.RxDogTag
import dagger.Component
import net.danlew.android.joda.JodaTimeAndroid
import okhttp3.OkHttpClient
import skedgo.tripgo.agenda.legacy.GetRoutingConfigModule
import skedgo.tripgo.agenda.legacy.WalkingSpeedRepositoryModule
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.Callable
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AutoCompleteTaskModule::class,
        AutoCompleteTaskProvidesModule::class,
        SchedulerFactoryModule::class,
        GooglePlacesModule::class,
        ConnectivityServiceModule::class,
        FetchSuggestionsModule::class,
        RouteStoreModule::class,
        TripKitUIModule::class,
        ContextModule::class,
        HttpClientModule::class,
        ErrorLoggerModule::class,
        PicassoModule::class,
        TripKitModule::class,
        DbHelperModule::class,
        ServiceViewModelModule::class,
        RealTimeRepositoryModule::class,
        ServiceAlertDataModule::class,
        ServiceDetailsModule::class,
        ServiceDetailItemViewModelModule::class,
        TripGroupRepositoryModule::class,
        DeparturesModule::class,
        EventTrackerModule::class,
        LocationStuffModule::class,
        MyPersonalDataModule::class,
        PreferredTransferTimeRepositoryModule::class,
        CyclingSpeedRepositoryModule::class,
        WalkingSpeedRepositoryModule::class,
        PrioritiesRepositoryModule::class,
        GetRoutingConfigModule::class,
        BookingModule::class,
        WaypointsModule::class,
        FavoriteTripsModule::class,
        FavoritesModule::class,
        UserInfoRepositoryModule::class,
        ViewModelModule::class,
        ControllerModule::class,
        DeveloperOptionModule::class,
        RemindersRepositoryModule::class,
        TripKitPreferenceModule::class
    ]
)
abstract class TripKitUI {
    abstract fun routeInputViewComponent(): RouteInputViewComponent

    abstract fun tripSegmentViewModelComponent(): TripSegmentViewModelComponent

    abstract fun timePickerComponent(): TimePickerComponent

    abstract fun tripDetailsComponent(): TripDetailsComponent

    abstract fun homeMapFragmentComponent(module: HomeMapFragmentModule): HomeMapFragmentComponent

    abstract fun serviceStopMapComponent(): ServiceStopMapComponent

    abstract fun routesComponent(): RoutesComponent

    abstract fun locationSearchComponent(): LocationSearchComponent

    abstract fun tripPreviewComponent(): TripPreviewComponent

    abstract fun autoCompleteRoutingComponent(): AutoCompleteRoutingComponent

    abstract fun locationPointerComponent(): LocationPointerComponent

    abstract fun controllerComponent(): ControllerComponent

    abstract fun bus(): Bus

    abstract fun httpClient(): OkHttpClient

    abstract fun appContext(): Context

    abstract fun regionService(): RegionService

    abstract fun fetchSuggestions(): FetchSuggestions

    abstract fun picasso(): Picasso

    abstract fun errorLogger(): ErrorLogger

    abstract fun searchRepository(): PlaceSearchRepository

    abstract fun dbHelper(): DbHelper

    abstract fun tripGroupRepository(): TripGroupRepository

    abstract fun unitsRepository(): UnitsRepository

    abstract fun routeStore(): RouteStore

    abstract fun regionRoutingRepository(): RegionRoutingRepository

    abstract fun regionRoutingAutoCompleter(): RegionRoutingAutoCompleter

    abstract fun inject(fragment: TimetableFragment)

    abstract fun inject(fragment: ServiceDetailFragment)

    abstract fun inject(fragment: PoiDetailsFragment)

    abstract fun inject(contributor: PoiDetailsMapContributor)

    companion object {
        var AUTHORITY_END: String = ".com.skedgo.tripkit.ui."
        private var instance: TripKitUI? = null

        @JvmStatic
        fun getInstance(): TripKitUI {
            synchronized(TripKitUI::class.java) {
                checkNotNull(instance) { "Must initialize TripKitUI before using getInstance()" }
                return instance!!
            }
        }

        fun buildTripKitConfig(context: Context, key: ApiKey?): Configs {
            val repository = DeveloperPreferenceRepositoryImpl(
                context, context.getSharedPreferences(
                    "TripKit", Context.MODE_PRIVATE
                )
            )
            val isDebuggable = (0 != (context.applicationInfo.flags
                and ApplicationInfo.FLAG_DEBUGGABLE) || BuildConfig.DEBUG)
            return builder().context(context)
                .debuggable(isDebuggable)
                .baseUrlAdapterFactory { repository.server }
                .userTokenProvider {
                    val prefs =
                        context.getSharedPreferences("UserTokenPreferences", Context.MODE_PRIVATE)
                    prefs.getString("userToken", "")
                }
                .key { key }.build()
        }

        fun buildTripKitConfig(
            context: Context,
            key: ApiKey,
            customUrlAdapterFactory: Callable<String>
        ): Configs {
            val repository = DeveloperPreferenceRepositoryImpl(
                context, context.getSharedPreferences(
                    "TripKit", Context.MODE_PRIVATE
                )
            )
            val isDebuggable = (0 != (context.applicationInfo.flags
                and ApplicationInfo.FLAG_DEBUGGABLE) || BuildConfig.DEBUG)
            return builder().context(context)
                .debuggable(isDebuggable)
                .baseUrlAdapterFactory(
                    if ((customUrlAdapterFactory != null)) customUrlAdapterFactory
                    else repository::server as Callable<String>

                )
                .userTokenProvider {
                    val prefs =
                        context.getSharedPreferences("UserTokenPreferences", Context.MODE_PRIVATE)
                    prefs.getString("userToken", "")
                }
                .key { key }.build()
        }

        fun initialize(
            context: Context, key: ApiKey,
            configs: Configs,
            httpClientModule: HttpClientModule,
            placesApiKey: String
        ) {
            initialize(context, key, configs, httpClientModule)
            if (!Places.isInitialized()) {
                Places.initialize(context, placesApiKey)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun initialize(
            context: Context, key: ApiKey,
            configs: Configs?,
            httpClientModule: HttpClientModule? = null
        ) {
            RxDogTag.install()
            MarkerIconManager.init(context)
            if (!isInitialized) {
                check("SKEDGO_API_KEY" != key.value) { "Invalid SkedGo API Key." }

                var tripKitConfigs = configs
                if (tripKitConfigs == null) {
                    tripKitConfigs = buildTripKitConfig(context, key)
                }

                if (httpClientModule != null) {
                    val tripKit = DaggerTripKit.builder()
                        .mainModule(MainModule(tripKitConfigs))
                        .httpClientModule(httpClientModule)
                        .build()
                    initialize(context, tripKit)
                    JodaTimeAndroid.init(context)
                    GetOffAlertCache.init(context)
                    GeoLocation.init(context)
                    if (VERSION.SDK_INT >= VERSION_CODES.O) {
                        val channels: MutableList<NotificationChannel> = ArrayList()
                        channels.add(
                            createChannel(
                                TripAlarmBroadcastReceiver.NOTIFICATION_CHANNEL_START_TRIP_ID,
                                TripAlarmBroadcastReceiver.NOTIFICATION_CHANNEL_START_TRIP
                            )
                        )
                        context
                            .createNotificationChannels(
                                channels
                            )
                    }
                } else {
                    initialize(tripKitConfigs)
                }

                if (!Places.isInitialized()) {
                    val placesApiKey = getGooglePlacesApiKey()
                    if (placesApiKey != null && !placesApiKey.isEmpty()) {
                        Places.initialize(context, placesApiKey)
                    }
                }

                if (tripKitConfigs.debuggable()) {
                    Timber.plant(DebugTree())
                }

                val builder: DaggerTripKitUI.Builder = DaggerTripKitUI.builder()
                if (httpClientModule != null) {
                    builder.httpClientModule(httpClientModule)
                } else {
                    builder.httpClientModule(
                        HttpClientModule(
                            null, null,
                            tripKitConfigs,
                            null, null
                        )
                    )
                }

                instance = builder.contextModule(ContextModule(context))
                    .build()
                DistanceFormatter.initialize(instance!!.unitsRepository())
            }
        }

        /**
         * Configure the font family used by TripKit Android UI Views.
         *
         * - Default (null) means "leave as is".
         * - Use `"sans-serif"` for the platform Roboto.
         */
        @JvmStatic
        fun setUIFontFamily(fontFamilyName: String?) {
            com.skedgo.tripkit.ui.core.TripKitUITypography.setFontFamilyName(fontFamilyName)
        }

        /**
         * Configure the font used by TripKit Android UI Views using a font resource.
         *
         * Example: `TripKitUI.setUIFont(R.font.roboto)`
         */
        @JvmStatic
        fun setUIFont(@androidx.annotation.FontRes fontResId: Int?) {
            com.skedgo.tripkit.ui.core.TripKitUITypography.setFont(fontResId)
        }
    }
}
