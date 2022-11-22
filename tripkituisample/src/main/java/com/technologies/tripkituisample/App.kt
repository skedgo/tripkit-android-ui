package com.technologies.tripkituisample

import android.app.Application
import com.skedgo.tripkit.HttpClientModule
import com.skedgo.tripkit.TripKitConfigs
import com.skedgo.tripkit.configuration.Key
import com.skedgo.tripkit.ui.TripKitUI

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val baseConfig = TripKitUI.buildTripKitConfig(applicationContext, Key.ApiKey(BuildConfig.SKEDGO_API_KEY))
        val httpClientModule = HttpClientModule(null, BuildConfig.VERSION_NAME, baseConfig, getSharedPreferences("MyPersonalData", MODE_PRIVATE))

        val appConfigs = TripKitConfigs.builder().from(baseConfig).build()
        TripKitUI.initialize(this, Key.ApiKey(BuildConfig.SKEDGO_API_KEY), appConfigs, httpClientModule)

    }
}