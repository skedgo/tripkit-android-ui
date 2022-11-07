package com.skedgo.tripkit.ui.core.module

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module

/**
 * For binding viewModels using dagger @Binds, @IntoMap and @ViewModelKey
 */
@Module
abstract class ViewModelModule {
    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

}
