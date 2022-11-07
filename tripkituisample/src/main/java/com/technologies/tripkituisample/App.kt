package com.technologies.tripkituisample

import android.app.Application
import com.skedgo.tripkit.HttpClientModule
import com.skedgo.tripkit.TripKitConfigs
import com.skedgo.tripkit.configuration.Key
import com.skedgo.tripkit.ui.TripKitUI

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val baseConfig = TripKitUI.buildTripKitConfig(applicationContext, Key.ApiKey("84aff3ca785a8bd99bb96c39324c7fa6"))
        val httpClientModule = HttpClientModule(null, BuildConfig.VERSION_NAME, baseConfig, getSharedPreferences("MyPersonalData", MODE_PRIVATE))

        val appConfigs = TripKitConfigs.builder().from(baseConfig).build()
        TripKitUI.initialize(this, Key.ApiKey("84aff3ca785a8bd99bb96c39324c7fa6"), appConfigs, httpClientModule)

    }
}