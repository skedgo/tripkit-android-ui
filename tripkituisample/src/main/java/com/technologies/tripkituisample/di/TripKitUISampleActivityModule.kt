package com.technologies.tripkituisample.di

import com.technologies.tripkituisample.autocompleter.AutocompleterActivity
import com.technologies.tripkituisample.location_search.LocationSearchActivity
import com.technologies.tripkituisample.routingresultview.RoutingResultActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class TripKitUISampleActivityModule {

    @ContributesAndroidInjector
    abstract fun contributeLocationSearchActivity(): LocationSearchActivity

    @ContributesAndroidInjector
    abstract fun contributeAutocompleterActivity(): AutocompleterActivity

    @ContributesAndroidInjector
    abstract fun contributeRoutingResultActivity(): RoutingResultActivity
}