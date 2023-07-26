package com.technologies.tripkituisample.di

import com.skedgo.tripkit.HttpClientModule
import com.skedgo.tripkit.account.data.AccountDataModule
import com.skedgo.tripkit.ui.core.module.*
import com.technologies.tripkituisample.App
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        TripKitUISampleActivityModule::class,
        TripKitUISampleModule::class,
        ContextModule::class,
        RouteStoreModule::class,
        TripGroupRepositoryModule::class,
        AccountDataModule::class,
        HttpClientModule::class,
        FavoriteTripsModule::class,
        LocationStuffModule::class,
        EventBusModule::class
    ]
)
abstract class TripKitUISampleComponent {
    abstract fun inject(application: App)

    @Component.Builder
    interface Builder {
        fun build(): TripKitUISampleComponent

        @BindsInstance
        fun applicationBind(application: App): Builder
        fun contextModule(contextModule: ContextModule): Builder
        fun httpClientModule(clientModule: HttpClientModule): Builder
    }
}