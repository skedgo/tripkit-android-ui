package com.technologies.tripkituisample

import android.app.Application
import com.skedgo.tripkit.HttpClientModule
import com.skedgo.tripkit.TripKitConfigs
import com.skedgo.tripkit.configuration.Key
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.module.ContextModule
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsDataBase
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsRepositoryImpl
import com.technologies.tripkituisample.di.DaggerTripKitUISampleComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class App : Application(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var tripKitUISampleActionButtonHandlerFactory: TripKitUISampleActionButtonHandlerFactory

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate() {
        super.onCreate()

        val baseConfig = TripKitUI.buildTripKitConfig(applicationContext, Key.ApiKey("84aff3ca785a8bd99bb96c39324c7fa6"))
        val httpClientModule = HttpClientModule(null, BuildConfig.VERSION_NAME, baseConfig, getSharedPreferences("MyPersonalData", MODE_PRIVATE))

        DaggerTripKitUISampleComponent.builder()
            .contextModule(ContextModule(this))
            .applicationBind(this)
            .httpClientModule(httpClientModule)
            .build()
            .inject(this)

        val appConfigs = TripKitUISampleConfigs.builder()
            .actionButtonHandlerFactory(tripKitUISampleActionButtonHandlerFactory)
            .from(baseConfig).build()
        TripKitUI.initialize(this, Key.ApiKey("84aff3ca785a8bd99bb96c39324c7fa6"), appConfigs, httpClientModule)
        TripKitUI.getInstance()
    }

}