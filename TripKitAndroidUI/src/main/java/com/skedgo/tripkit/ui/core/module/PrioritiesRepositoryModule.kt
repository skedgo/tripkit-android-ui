package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.routing.settings.PrioritiesRepository
import com.skedgo.tripkit.ui.routing.settings.PrioritiesRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class PrioritiesRepositoryModule {
    @Binds
    internal abstract fun prioritiesRepository(
        impl: PrioritiesRepositoryImpl
    ): PrioritiesRepository
}
