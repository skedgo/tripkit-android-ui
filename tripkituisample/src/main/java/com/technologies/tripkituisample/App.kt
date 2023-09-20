package com.technologies.tripkituisample

import android.app.Application
import com.skedgo.tripkit.DateTimePickerConfig
import com.skedgo.tripkit.HttpClientModule
import com.skedgo.tripkit.TripKitConfigs
import com.skedgo.tripkit.configuration.Key
import com.skedgo.tripkit.data.HttpClientCustomDataStore
import com.skedgo.tripkit.ui.TripKitUI
import com.uber.rxdogtag.RxDogTag
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber
import java.util.concurrent.Callable
import javax.inject.Inject

class App : Application(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate() {
        super.onCreate()

        RxDogTag.install()
        RxJavaPlugins.setErrorHandler { e ->
            Timber.e(e)
        }

        val tripGoApiKey = Key.ApiKey("YOUR_TRIPGO_API_KEY_HERE") //BuildConfig.tripgo_api_key

        val baseUrlAdapterFactory: Callable<String> = Callable {
            // Your code here to generate the custom base URL
            "https://example.com/"
        }

        val baseConfig = TripKitUI.buildTripKitConfig(
            applicationContext,
            tripGoApiKey
            /*, baseUrlAdapterFactory*/ //Add here your baseUrlAdapterFactory for your custom url
        )

        val httpClientModule = HttpClientModule(
            null,
            BuildConfig.VERSION_NAME,
            baseConfig,
            getSharedPreferences("MyPersonalData", MODE_PRIVATE)
        )

        HttpClientCustomDataStore.apply {
            init(this@App)
            setCustomHeaders(
                mapOf(
                    "CustomHeader1" to "CustomHeader1Value",
                    "CustomHeader2" to "CustomHeader1Value2",
                    "CustomHeader3" to "CustomHeader1Value3",
                )
            )
        }

        val dateTimePickerConfig = DateTimePickerConfig(
            getString(R.string.leave_at),
            getString(R.string.arrive_by)
        )

        val appConfigs =
            TripKitConfigs.builder().from(baseConfig)
                .dateTimePickerConfig(dateTimePickerConfig)
                .build()

        TripKitUI.initialize(
            this,
            tripGoApiKey,
            appConfigs,
            httpClientModule,
            //BuildConfig.google_map_key
            "YOUR_GOOGLE_API_KEY" //Adding your google api key here to initialize google Places API
        )

    }
}