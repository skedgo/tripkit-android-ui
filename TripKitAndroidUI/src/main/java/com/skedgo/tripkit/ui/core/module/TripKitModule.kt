package com.skedgo.tripkit.ui.core.module

import android.content.Context
import android.content.SharedPreferences
import com.skedgo.TripKit
import com.skedgo.tripkit.DefaultTripPreferences
import com.skedgo.tripkit.TripKitConstants
import com.skedgo.tripkit.TripPreferences
import com.skedgo.tripkit.a2brouting.RouteService
import com.skedgo.tripkit.a2brouting.SingleRouteService
import com.skedgo.tripkit.agenda.ConfigRepository
import com.skedgo.tripkit.analytics.MarkTripAsPlannedWithUserInfo
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.ui.core.ConfigCreator
import com.skedgo.tripkit.ui.core.settings.BaseUrlAdapter
import dagger.Module
import dagger.Provides
import java.util.concurrent.Callable
import javax.inject.Singleton

@Module
class TripKitModule {
    @Provides
    internal fun regionService(): RegionService = TripKit.getInstance().regionService

    @Provides
    @Singleton
    internal fun routeService(): RouteService =
        SingleRouteService(TripKit.getInstance().routeService)

    @Provides
    internal fun locationInfoService(): com.skedgo.tripkit.LocationInfoService =
        TripKit.getInstance().locationInfoService

    @Provides
    internal fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            TripKitConstants.PREF_NAME_TRIP_KIT,
            Context.MODE_PRIVATE
        )
    }

    @Provides
    internal fun tripPreferences(context: Context): com.skedgo.tripkit.TripPreferences =
        DefaultTripPreferences(
            context.getSharedPreferences(
                "TripPreferences", Context.MODE_PRIVATE
            )
        )

    @Provides
    internal fun tripPreferencesFactory(tripPreferences: com.skedgo.tripkit.TripPreferences):
        Callable<com.skedgo.tripkit.TripPreferences> = Callable { tripPreferences }

    @Provides
    internal fun markTripAsPlannedWithUserInfo(): MarkTripAsPlannedWithUserInfo =
        TripKit.getInstance().analyticsComponent().markTripAsPlannedWithUserInfo

    @Provides
    internal fun baseUrlAdapterFactory(baseUrlAdapter: BaseUrlAdapter):
        Callable<Callable<String>> = Callable { baseUrlAdapter }

    @Provides
    fun printTime(): PrintTime = TripKit.getInstance().dateTimeComponent().printTime

    @Provides
    internal fun configCreator(
        context: Context,
        preferences: SharedPreferences,
        tripPreferences: TripPreferences
    ): ConfigRepository {
        return ConfigCreator(
            context,
            preferences,
            "11",  // TODO
            tripPreferences
        )
    }

}