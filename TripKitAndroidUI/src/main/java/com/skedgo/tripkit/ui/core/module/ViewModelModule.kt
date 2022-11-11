package com.skedgo.tripkit.ui.core.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.routing.autocompleter.RouteAutocompleteViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * For binding viewModels using dagger @Binds, @IntoMap and @ViewModelKey
 */
@Module
abstract class ViewModelModule {
    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(RouteAutocompleteViewModel::class)
    internal abstract fun bindRouteAutocompleteViewModel(viewModel: RouteAutocompleteViewModel): ViewModel

}
