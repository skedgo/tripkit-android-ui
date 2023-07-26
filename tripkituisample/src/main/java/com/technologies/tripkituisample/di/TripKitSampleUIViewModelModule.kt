package com.technologies.tripkituisample.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ViewModelFactory
import com.skedgo.tripkit.ui.core.module.ViewModelKey
import com.technologies.tripkituisample.autocompleter.AutocompleterViewModel
import com.technologies.tripkituisample.routingresultview.RoutingResultViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class TripKitSampleUIViewModelModule {
    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(AutocompleterViewModel::class)
    internal abstract fun bindAutocompleterViewModel(viewModel: AutocompleterViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RoutingResultViewModel::class)
    internal abstract fun bindRoutingResultViewModel(viewModel: RoutingResultViewModel): ViewModel
}